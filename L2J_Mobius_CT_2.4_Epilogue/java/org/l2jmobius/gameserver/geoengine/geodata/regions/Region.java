/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.geoengine.geodata.regions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.l2jmobius.gameserver.config.GeoEngineConfig;
import org.l2jmobius.gameserver.geoengine.geodata.Cell;
import org.l2jmobius.gameserver.geoengine.geodata.IBlock;
import org.l2jmobius.gameserver.geoengine.geodata.IRegion;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.ComplexBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.FlatBlock;
import org.l2jmobius.gameserver.geoengine.geodata.blocks.MultilayerBlock;

/**
 * Off-heap Project Panama {@link MemorySegment} backed Geodata Region.<br>
 * Eliminates ~115,000 Java heap objects per region by encoding 65,536 block descriptors into a single primitive {@code int[]} and indexing multilayer cell offsets in $O(1)$ time, while lazily materializing copy-on-write {@link IBlock} overrides only for blocks dynamically mutated by doors, fences, or geo-editing.
 * @author HorridoJoho, Mobius, dwea
 */
public class Region implements IRegion
{
	private static final ValueLayout.OfShort SHORT_LE = ValueLayout.JAVA_SHORT_UNALIGNED.withOrder(ByteOrder.LITTLE_ENDIAN);
	private static final int[] EMPTY_INTS = new int[0];
	
	private static final int DESC_TYPE_SHIFT = 30;
	private static final int DESC_PAYLOAD_MASK = 0x3FFFFFFF;
	private static final int DESC_FLAT = 0;
	private static final int DESC_COMPLEX = 1;
	private static final int DESC_MULTILAYER = 2;
	private static final int DESC_MUTATED = 3;
	
	private final int[] _blockDescriptors = new int[IRegion.REGION_BLOCKS];
	private volatile MemorySegment _segment;
	private int[] _mlCellOffsets = EMPTY_INTS;
	private IBlock[] _mutatedBlocks;
	private int _mutatedCount;
	
	public Region(MemorySegment segment)
	{
		load(segment);
	}
	
	public Region(ByteBuffer bb)
	{
		load(bb);
	}
	
	public synchronized void load(ByteBuffer bb)
	{
		load(MemorySegment.ofBuffer(bb));
	}
	
	public synchronized void load(MemorySegment segment)
	{
		int[] mlOffsets = new int[4096 * IBlock.BLOCK_CELLS];
		int mlBlockCount = 0;
		long pos = 0;
		
		for (int blockOffset = 0; blockOffset < IRegion.REGION_BLOCKS; blockOffset++)
		{
			final byte blockType = segment.get(ValueLayout.JAVA_BYTE, pos++);
			switch (blockType)
			{
				case IBlock.TYPE_FLAT ->
				{
					final short height = segment.get(SHORT_LE, pos);
					pos += 2;
					_blockDescriptors[blockOffset] = (DESC_FLAT << DESC_TYPE_SHIFT) | (height & 0xFFFF);
				}
				case IBlock.TYPE_COMPLEX ->
				{
					_blockDescriptors[blockOffset] = (DESC_COMPLEX << DESC_TYPE_SHIFT) | ((int) pos & DESC_PAYLOAD_MASK);
					pos += IBlock.BLOCK_CELLS * 2L;
				}
				case IBlock.TYPE_MULTILAYER ->
				{
					final int mlBaseIdx = mlBlockCount << 6;
					if ((mlBaseIdx + IBlock.BLOCK_CELLS) > mlOffsets.length)
					{
						mlOffsets = Arrays.copyOf(mlOffsets, mlOffsets.length << 1);
					}
					
					_blockDescriptors[blockOffset] = (DESC_MULTILAYER << DESC_TYPE_SHIFT) | (mlBaseIdx & DESC_PAYLOAD_MASK);
					for (int cellOffset = 0; cellOffset < IBlock.BLOCK_CELLS; cellOffset++)
					{
						mlOffsets[mlBaseIdx | cellOffset] = (int) pos;
						final byte nLayers = segment.get(ValueLayout.JAVA_BYTE, pos);
						if ((nLayers <= 0) || (nLayers > 125))
						{
							throw new RuntimeException("L2JGeoDriver: Geo file corrupted! Invalid layers count: " + nLayers);
						}
						
						pos += 1 + ((nLayers & 0xFF) << 1);
					}
					
					mlBlockCount++;
				}
				default -> throw new RuntimeException("Invalid block type " + blockType + "!");
			}
		}
		
		_segment = segment;
		_mlCellOffsets = (mlBlockCount == 0) ? EMPTY_INTS : Arrays.copyOf(mlOffsets, mlBlockCount << 6);
		_mutatedBlocks = null;
		_mutatedCount = 0;
	}
	
	private static int getBlockIndex(int geoX, int geoY)
	{
		return (((geoX >> 3) & 0xFF) << 8) | ((geoY >> 3) & 0xFF);
	}
	
	private static int getCellIndex(int geoX, int geoY)
	{
		return ((geoX & 7) << 3) | (geoY & 7);
	}
	
	private short getNearestLayerData(int mlBaseIdx, int geoX, int geoY, int worldZ)
	{
		final MemorySegment seg = _segment;
		final int startOffset = _mlCellOffsets[mlBaseIdx | getCellIndex(geoX, geoY)];
		final int nLayers = seg.get(ValueLayout.JAVA_BYTE, startOffset) & 0xFF;
		final int endOffset = startOffset + 1 + (nLayers << 1);
		
		int nearestDZ = 0;
		short nearestData = 0;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = seg.get(SHORT_LE, offset);
			final int layerZ = (short) (layerData & 0x0FFF0) >> 1;
			if (layerZ == worldZ)
			{
				return layerData;
			}
			
			final int layerDZ = Math.abs(layerZ - worldZ);
			if ((offset == (startOffset + 1)) || (layerDZ < nearestDZ))
			{
				nearestDZ = layerDZ;
				nearestData = layerData;
			}
		}
		
		return nearestData;
	}
	
	private int getMultilayerNextLowerZ(int mlBaseIdx, int geoX, int geoY, int worldZ)
	{
		final MemorySegment seg = _segment;
		final int startOffset = _mlCellOffsets[mlBaseIdx | getCellIndex(geoX, geoY)];
		final int nLayers = seg.get(ValueLayout.JAVA_BYTE, startOffset) & 0xFF;
		final int endOffset = startOffset + 1 + (nLayers << 1);
		
		int lowerZ = Integer.MIN_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = seg.get(SHORT_LE, offset);
			final int layerZ = (short) (layerData & 0x0FFF0) >> 1;
			if (layerZ == worldZ)
			{
				return layerZ;
			}
			
			if ((layerZ < worldZ) && (layerZ > lowerZ))
			{
				lowerZ = layerZ;
			}
		}
		
		return lowerZ == Integer.MIN_VALUE ? worldZ : lowerZ;
	}
	
	private int getMultilayerNextHigherZ(int mlBaseIdx, int geoX, int geoY, int worldZ)
	{
		final MemorySegment seg = _segment;
		final int startOffset = _mlCellOffsets[mlBaseIdx | getCellIndex(geoX, geoY)];
		final int nLayers = seg.get(ValueLayout.JAVA_BYTE, startOffset) & 0xFF;
		final int endOffset = startOffset + 1 + (nLayers << 1);
		
		int higherZ = Integer.MAX_VALUE;
		for (int offset = startOffset + 1; offset < endOffset; offset += 2)
		{
			final short layerData = seg.get(SHORT_LE, offset);
			final int layerZ = (short) (layerData & 0x0FFF0) >> 1;
			if (layerZ == worldZ)
			{
				return layerZ;
			}
			
			if ((layerZ > worldZ) && (layerZ < higherZ))
			{
				higherZ = layerZ;
			}
		}
		
		return higherZ == Integer.MAX_VALUE ? worldZ : higherZ;
	}
	
	private synchronized IBlock getOrMaterializeBlock(int blockIdx, boolean convertFlat)
	{
		final int desc = _blockDescriptors[blockIdx];
		final int type = desc >>> DESC_TYPE_SHIFT;
		final int payload = desc & DESC_PAYLOAD_MASK;
		if (type == DESC_MUTATED)
		{
			final IBlock existing = _mutatedBlocks[payload];
			if (convertFlat && (existing instanceof FlatBlock flat))
			{
				final IBlock converted = createComplexFromFlat(flat.getHeight());
				_mutatedBlocks[payload] = converted;
				return converted;
			}
			return existing;
		}
		
		if ((type == DESC_FLAT) && !convertFlat)
		{
			return null;
		}
		
		final IBlock materialized = switch (type)
		{
			case DESC_FLAT -> createComplexFromFlat((short) desc);
			case DESC_COMPLEX ->
			{
				final short[] data = new short[IBlock.BLOCK_CELLS];
				for (int i = 0; i < IBlock.BLOCK_CELLS; i++)
				{
					data[i] = _segment.get(SHORT_LE, payload + (i << 1));
				}
				yield new ComplexBlock(data);
			}
			case DESC_MULTILAYER ->
			{
				final int startOffset = _mlCellOffsets[payload];
				final int cell63Offset = _mlCellOffsets[payload | 63];
				final int cell63Layers = _segment.get(ValueLayout.JAVA_BYTE, cell63Offset) & 0xFF;
				final int byteLength = (cell63Offset + 1 + (cell63Layers << 1)) - startOffset;
				final byte[] data = _segment.asSlice(startOffset, byteLength).toArray(ValueLayout.JAVA_BYTE);
				yield new MultilayerBlock(data);
			}
			default -> throw new IllegalStateException("Unexpected descriptor type: " + type);
		};
		
		if (_mutatedBlocks == null)
		{
			_mutatedBlocks = new IBlock[16];
		}
		else if (_mutatedCount >= _mutatedBlocks.length)
		{
			_mutatedBlocks = Arrays.copyOf(_mutatedBlocks, _mutatedBlocks.length << 1);
		}
		
		final int mutatedIdx = _mutatedCount++;
		_mutatedBlocks[mutatedIdx] = materialized;
		_blockDescriptors[blockIdx] = (DESC_MUTATED << DESC_TYPE_SHIFT) | (mutatedIdx & DESC_PAYLOAD_MASK);
		return materialized;
	}
	
	private static ComplexBlock createComplexFromFlat(short currentHeight)
	{
		final short encodedHeight = (short) ((currentHeight << 1) & 0xFFFF);
		final short combinedData = (short) (encodedHeight | Cell.NSWE_ALL);
		final short[] data = new short[IBlock.BLOCK_CELLS];
		Arrays.fill(data, combinedData);
		return new ComplexBlock(data);
	}
	
	@Override
	public boolean checkNearestNswe(int geoX, int geoY, int worldZ, int nswe)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT -> true;
			case DESC_COMPLEX -> (_segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1)) & nswe) == nswe;
			case DESC_MULTILAYER -> (getNearestLayerData(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ) & nswe) == nswe;
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].checkNearestNswe(geoX, geoY, worldZ, nswe);
		};
	}
	
	@Override
	public short getNearestNswe(int geoX, int geoY, int worldZ)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT -> Cell.NSWE_ALL;
			case DESC_COMPLEX -> (short) (_segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1)) & Cell.NSWE_ALL);
			case DESC_MULTILAYER -> (short) (getNearestLayerData(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ) & Cell.NSWE_ALL);
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].getNearestNswe(geoX, geoY, worldZ);
		};
	}
	
	@Override
	public void setNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final int blockIdx = getBlockIndex(geoX, geoY);
		final IBlock block = getOrMaterializeBlock(blockIdx, false);
		if (block != null)
		{
			block.setNearestNswe(geoX, geoY, worldZ, nswe);
		}
	}
	
	@Override
	public void unsetNearestNswe(int geoX, int geoY, int worldZ, byte nswe)
	{
		final int blockIdx = getBlockIndex(geoX, geoY);
		final IBlock block = getOrMaterializeBlock(blockIdx, true);
		if (block != null)
		{
			block.unsetNearestNswe(geoX, geoY, worldZ, nswe);
		}
	}
	
	@Override
	public int getNearestZ(int geoX, int geoY, int worldZ)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT -> (short) desc;
			case DESC_COMPLEX -> (short) (_segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1)) & 0x0FFF0) >> 1;
			case DESC_MULTILAYER ->
			{
				final int nearestZ = (short) (getNearestLayerData(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ) & 0x0FFF0) >> 1;
				yield ((nearestZ - worldZ) > 1000) ? getMultilayerNextLowerZ(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ) : nearestZ;
			}
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].getNearestZ(geoX, geoY, worldZ);
		};
	}
	
	@Override
	public int getNextLowerZ(int geoX, int geoY, int worldZ)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT ->
			{
				final int h = (short) desc;
				yield h <= worldZ ? h : worldZ;
			}
			case DESC_COMPLEX ->
			{
				final int h = (short) (_segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1)) & 0x0FFF0) >> 1;
				yield h <= worldZ ? h : worldZ;
			}
			case DESC_MULTILAYER -> getMultilayerNextLowerZ(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ);
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].getNextLowerZ(geoX, geoY, worldZ);
		};
	}
	
	@Override
	public int getNextHigherZ(int geoX, int geoY, int worldZ)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT ->
			{
				final int h = (short) desc;
				yield h >= worldZ ? h : worldZ;
			}
			case DESC_COMPLEX ->
			{
				final int h = (short) (_segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1)) & 0x0FFF0) >> 1;
				yield h >= worldZ ? h : worldZ;
			}
			case DESC_MULTILAYER -> getMultilayerNextHigherZ(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ);
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].getNextHigherZ(geoX, geoY, worldZ);
		};
	}
	
	@Override
	public int getNearestZAndNswe(int geoX, int geoY, int worldZ)
	{
		final int desc = _blockDescriptors[getBlockIndex(geoX, geoY)];
		return switch (desc >>> DESC_TYPE_SHIFT)
		{
			case DESC_FLAT -> (((short) desc) << 4) | Cell.NSWE_ALL;
			case DESC_COMPLEX ->
			{
				final short cellData = _segment.get(SHORT_LE, (desc & DESC_PAYLOAD_MASK) + (getCellIndex(geoX, geoY) << 1));
				final int h = (short) (cellData & 0x0FFF0) >> 1;
				yield (h << 4) | (cellData & Cell.NSWE_ALL);
			}
			case DESC_MULTILAYER ->
			{
				final short layerData = getNearestLayerData(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ);
				final int nearestZ = (short) (layerData & 0x0FFF0) >> 1;
				final int resolvedZ = ((nearestZ - worldZ) > 1000) ? getMultilayerNextLowerZ(desc & DESC_PAYLOAD_MASK, geoX, geoY, worldZ) : nearestZ;
				yield (resolvedZ << 4) | (layerData & Cell.NSWE_ALL);
			}
			default -> _mutatedBlocks[desc & DESC_PAYLOAD_MASK].getNearestZAndNswe(geoX, geoY, worldZ);
		};
	}
	
	@Override
	public boolean hasGeo()
	{
		return true;
	}
	
	@Override
	public synchronized boolean saveToFile(String fileName)
	{
		final Path filePath = new File(GeoEngineConfig.GEOEDIT_PATH + File.separator + fileName).toPath();
		if (Files.exists(filePath))
		{
			try
			{
				Files.delete(filePath);
			}
			catch (IOException e)
			{
				return false;
			}
		}
		
		try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile())))
		{
			for (int blockOffset = 0; blockOffset < IRegion.REGION_BLOCKS; blockOffset++)
			{
				final int desc = _blockDescriptors[blockOffset];
				final int type = desc >>> DESC_TYPE_SHIFT;
				final int payload = desc & DESC_PAYLOAD_MASK;
				switch (type)
				{
					case DESC_FLAT ->
					{
						final short height = (short) desc;
						bos.write(IBlock.TYPE_FLAT);
						bos.write(height & 0xFF);
						bos.write((height >> 8) & 0xFF);
					}
					case DESC_COMPLEX ->
					{
						bos.write(IBlock.TYPE_COMPLEX);
						final byte[] raw = _segment.asSlice(payload, IBlock.BLOCK_CELLS * 2L).toArray(ValueLayout.JAVA_BYTE);
						bos.write(raw);
					}
					case DESC_MULTILAYER ->
					{
						bos.write(IBlock.TYPE_MULTILAYER);
						final int startOffset = _mlCellOffsets[payload];
						final int cell63Offset = _mlCellOffsets[payload | 63];
						final int cell63Layers = _segment.get(ValueLayout.JAVA_BYTE, cell63Offset) & 0xFF;
						final int byteLength = (cell63Offset + 1 + (cell63Layers << 1)) - startOffset;
						final byte[] raw = _segment.asSlice(startOffset, byteLength).toArray(ValueLayout.JAVA_BYTE);
						bos.write(raw);
					}
					case DESC_MUTATED ->
					{
						final IBlock block = _mutatedBlocks[payload];
						if (block instanceof FlatBlock flat)
						{
							final short height = flat.getHeight();
							bos.write(IBlock.TYPE_FLAT);
							bos.write(height & 0xFF);
							bos.write((height >> 8) & 0xFF);
						}
						else if (block instanceof ComplexBlock complex)
						{
							final short[] data = complex.getData();
							final ByteBuffer buffer = ByteBuffer.allocate(1 + (data.length * 2));
							buffer.put((byte) IBlock.TYPE_COMPLEX);
							for (short info : data)
							{
								buffer.putShort(Short.reverseBytes(info));
							}
							bos.write(buffer.array());
						}
						else if (block instanceof MultilayerBlock ml)
						{
							final byte[] data = ml.getData();
							bos.write(IBlock.TYPE_MULTILAYER);
							bos.write(data);
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			return false;
		}
		
		return true;
	}
}
