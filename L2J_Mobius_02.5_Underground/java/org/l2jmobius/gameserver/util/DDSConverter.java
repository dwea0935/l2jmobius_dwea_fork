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
package org.l2jmobius.gameserver.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

public class DDSConverter
{
	private static final Logger LOGGER = Logger.getLogger(DDSConverter.class.getName());
	
	protected static class Color
	{
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			
			if ((obj == null) || (getClass() != obj.getClass()))
			{
				return false;
			}
			
			final Color color = (Color) obj;
			if (b != color.b)
			{
				return false;
			}
			
			if (g != color.g)
			{
				return false;
			}
			
			return r == color.r;
		}
		
		@Override
		public int hashCode()
		{
			int i = r;
			i = (29 * i) + g;
			i = (29 * i) + b;
			return i;
		}
		
		protected int r;
		protected int g;
		protected int b;
		
		public Color()
		{
			r = g = b = 0;
		}
		
		public Color(int i, int j, int k)
		{
			r = i;
			g = j;
			b = k;
		}
	}
	
	public static ByteBuffer convertToDDS(BufferedImage bufferedimage)
	{
		if (bufferedimage == null)
		{
			return null;
		}
		
		if (bufferedimage.getColorModel().hasAlpha())
		{
			return convertToDxt3(bufferedimage);
		}
		
		return convertToDxt1NoTransparency(bufferedimage);
	}
	
	public static ByteBuffer convertToDDS(File file)
	{
		if (file == null)
		{
			String s = "nullValue.FileIsNull";
			LOGGER.severe(s + " " + new IllegalArgumentException(s));
			new IllegalArgumentException(s);
			return null;
		}
		
		if (!file.exists() || !file.canRead())
		{
			String s1 = "DDSConverter.NoFileOrNoPermission";
			LOGGER.severe(s1 + " " + new IllegalArgumentException(s1));
			new IllegalArgumentException(s1);
			return null;
		}
		
		BufferedImage bufferedimage = null;
		try
		{
			bufferedimage = ImageIO.read(file);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			// LOG.error("Error while reading Image that needs to be DDSConverted", e);
		}
		
		if (bufferedimage == null)
		{
			return null;
		}
		
		if (bufferedimage.getColorModel().hasAlpha())
		{
			return convertToDxt3(bufferedimage);
		}
		
		return convertToDxt1NoTransparency(bufferedimage);
	}
	
	public static ByteBuffer convertToDxt1NoTransparency(BufferedImage bufferedimage)
	{
		if (bufferedimage == null)
		{
			return null;
		}
		
		final int[] ai = new int[16];
		final int i = 128 + ((bufferedimage.getWidth() * bufferedimage.getHeight()) / 2);
		final ByteBuffer bytebuffer = ByteBuffer.allocate(i);
		bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
		buildHeaderDxt1(bytebuffer, bufferedimage.getWidth(), bufferedimage.getHeight());
		final int j = bufferedimage.getWidth() / 4;
		final int k = bufferedimage.getHeight() / 4;
		for (int l = 0; l < k; l++)
		{
			for (int i1 = 0; i1 < j; i1++)
			{
				final BufferedImage bufferedimage1 = bufferedimage.getSubimage(i1 * 4, l * 4, 4, 4);
				bufferedimage1.getRGB(0, 0, 4, 4, ai, 0, 4);
				final Color[] acolor = getColors888(ai);
				for (int j1 = 0; j1 < ai.length; j1++)
				{
					ai[j1] = getPixel565(acolor[j1]);
					acolor[j1] = getColor565(ai[j1]);
				}
				
				final int[] ai1 = determineExtremeColors(acolor);
				if (ai[ai1[0]] < ai[ai1[1]])
				{
					final int k1 = ai1[0];
					ai1[0] = ai1[1];
					ai1[1] = k1;
				}
				bytebuffer.putShort((short) ai[ai1[0]]);
				bytebuffer.putShort((short) ai[ai1[1]]);
				final long l1 = computeBitMask(acolor, ai1);
				bytebuffer.putInt((int) l1);
			}
			
		}
		
		return bytebuffer;
	}
	
	public static ByteBuffer convertToDxt3(BufferedImage bufferedimage)
	{
		if (bufferedimage == null)
		{
			return null;
		}
		
		if (!bufferedimage.getColorModel().hasAlpha())
		{
			return convertToDxt1NoTransparency(bufferedimage);
		}
		
		final int[] ai = new int[16];
		final int i = 128 + (bufferedimage.getWidth() * bufferedimage.getHeight());
		final ByteBuffer bytebuffer = ByteBuffer.allocate(i);
		bytebuffer.order(ByteOrder.LITTLE_ENDIAN);
		buildHeaderDxt3(bytebuffer, bufferedimage.getWidth(), bufferedimage.getHeight());
		final int j = bufferedimage.getWidth() / 4;
		final int k = bufferedimage.getHeight() / 4;
		for (int l = 0; l < k; l++)
		{
			for (int i1 = 0; i1 < j; i1++)
			{
				final BufferedImage bufferedimage1 = bufferedimage.getSubimage(i1 * 4, l * 4, 4, 4);
				bufferedimage1.getRGB(0, 0, 4, 4, ai, 0, 4);
				final Color[] acolor = getColors888(ai);
				for (int j1 = 0; j1 < ai.length; j1 += 2)
				{
					bytebuffer.put((byte) ((ai[j1] >>> 28) | (ai[j1 + 1] >>> 24)));
				}
				
				for (int k1 = 0; k1 < ai.length; k1++)
				{
					ai[k1] = getPixel565(acolor[k1]);
					acolor[k1] = getColor565(ai[k1]);
				}
				
				final int[] ai1 = determineExtremeColors(acolor);
				if (ai[ai1[0]] < ai[ai1[1]])
				{
					int l1 = ai1[0];
					ai1[0] = ai1[1];
					ai1[1] = l1;
				}
				bytebuffer.putShort((short) ai[ai1[0]]);
				bytebuffer.putShort((short) ai[ai1[1]]);
				final long l2 = computeBitMask(acolor, ai1);
				bytebuffer.putInt((int) l2);
			}
		}
		
		return bytebuffer;
	}
	
	protected static void buildHeaderDxt1(ByteBuffer bytebuffer, int i, int j)
	{
		bytebuffer.rewind();
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 83);
		bytebuffer.put((byte) 32);
		bytebuffer.putInt(124);
		final int k = 0xa1007;
		bytebuffer.putInt(k);
		bytebuffer.putInt(j);
		bytebuffer.putInt(i);
		bytebuffer.putInt((i * j) / 2);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.position(bytebuffer.position() + 44);
		bytebuffer.putInt(32);
		bytebuffer.putInt(4);
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 88);
		bytebuffer.put((byte) 84);
		bytebuffer.put((byte) 49);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(4096);
		bytebuffer.putInt(0);
		bytebuffer.position(bytebuffer.position() + 12);
	}
	
	protected static void buildHeaderDxt3(ByteBuffer bytebuffer, int i, int j)
	{
		bytebuffer.rewind();
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 83);
		bytebuffer.put((byte) 32);
		bytebuffer.putInt(124);
		final int k = 0xa1007;
		bytebuffer.putInt(k);
		bytebuffer.putInt(j);
		bytebuffer.putInt(i);
		bytebuffer.putInt(i * j);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.position(bytebuffer.position() + 44);
		bytebuffer.putInt(32);
		bytebuffer.putInt(4);
		bytebuffer.put((byte) 68);
		bytebuffer.put((byte) 88);
		bytebuffer.put((byte) 84);
		bytebuffer.put((byte) 51);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(0);
		bytebuffer.putInt(4096);
		bytebuffer.putInt(0);
		bytebuffer.position(bytebuffer.position() + 12);
	}
	
	protected static int[] determineExtremeColors(Color acolor[])
	{
		int i = 0x80000000;
		final int[] ai = new int[2];
		for (int j = 0; j < (acolor.length - 1); j++)
		{
			for (int k = j + 1; k < acolor.length; k++)
			{
				int l = distance(acolor[j], acolor[k]);
				if (l > i)
				{
					i = l;
					ai[0] = j;
					ai[1] = k;
				}
			}
		}
		return ai;
	}
	
	protected static long computeBitMask(Color[] acolor, int[] ai)
	{
		final Color[] acolor1 =
		{
			null,
			null,
			new Color(),
			new Color()
		};
		
		acolor1[0] = acolor[ai[0]];
		acolor1[1] = acolor[ai[1]];
		if (acolor1[0].equals(acolor1[1]))
		{
			return 0L;
		}
		
		acolor1[2].r = ((2 * acolor1[0].r) + acolor1[1].r + 1) / 3;
		acolor1[2].g = ((2 * acolor1[0].g) + acolor1[1].g + 1) / 3;
		acolor1[2].b = ((2 * acolor1[0].b) + acolor1[1].b + 1) / 3;
		acolor1[3].r = (acolor1[0].r + (2 * acolor1[1].r) + 1) / 3;
		acolor1[3].g = (acolor1[0].g + (2 * acolor1[1].g) + 1) / 3;
		acolor1[3].b = (acolor1[0].b + (2 * acolor1[1].b) + 1) / 3;
		
		long l = 0L;
		for (int i = 0; i < acolor.length; i++)
		{
			int j = 0x7fffffff;
			int k = 0;
			for (int i1 = 0; i1 < acolor1.length; i1++)
			{
				int j1 = distance(acolor[i], acolor1[i1]);
				if (j1 < j)
				{
					j = j1;
					k = i1;
				}
			}
			
			l |= k << (i * 2);
		}
		
		return l;
	}
	
	protected static int getPixel565(Color color)
	{
		final int i = color.r >> 3;
		final int j = color.g >> 2;
		final int k = color.b >> 3;
		return (i << 11) | (j << 5) | k;
	}
	
	protected static Color getColor565(int i)
	{
		final Color color = new Color();
		color.r = (int) (i & 63488L) >> 11;
		color.g = (int) (i & 2016L) >> 5;
		color.b = (int) (i & 31L);
		return color;
	}
	
	protected static Color[] getColors888(int ai[])
	{
		final Color[] acolor = new Color[ai.length];
		for (int i = 0; i < ai.length; i++)
		{
			acolor[i] = new Color();
			acolor[i].r = (int) (ai[i] & 0xff0000L) >> 16;
			acolor[i].g = (int) (ai[i] & 65280L) >> 8;
			acolor[i].b = (int) (ai[i] & 255L);
		}
		return acolor;
	}
	
	protected static int distance(Color color, Color color1)
	{
		return ((color1.r - color.r) * (color1.r - color.r)) + ((color1.g - color.g) * (color1.g - color.g)) + ((color1.b - color.b) * (color1.b - color.b));
	}
}
