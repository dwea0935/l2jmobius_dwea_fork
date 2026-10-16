#!/bin/sh
# Container entrypoint for the L2J Mobius Login/Game servers.
# Usage: run-server.sh login|game
# Equivalent to dist/<login|game>/*ServerTask.sh, but logs to stdout (docker logs)
# and leaves restart handling to Docker (restart: on-failure).
set -e

case "$1" in
	login) JAR=LoginServer.jar; MAIN=org.l2jmobius.loginserver.LoginServer ;;
	game)  JAR=GameServer.jar;  MAIN=org.l2jmobius.gameserver.GameServer ;;
	*) echo "usage: $0 login|game" >&2; exit 64 ;;
esac

cd "/opt/l2/$1"
mkdir -p log

# java.cfg ships with CRLF line endings; strip them before word-splitting.
JAVA_OPTS="$(tr -d '\r' < java.cfg)"

# Explicit classpath + main class instead of `-jar`, so a server jar that was repacked
# without its Main-Class/Class-Path manifest still starts. Only the selected server jar
# plus the third-party libs are included (never both server jars, to avoid class clashes).
CP="../libs/$JAR"
for lib in ../libs/*.jar; do
	case "$lib" in */GameServer.jar|*/LoginServer.jar) ;; *) CP="$CP:$lib" ;; esac
done

# -Djava.awt.headless=true disables the Swing GUI (InterfaceConfig checks GraphicsEnvironment.isHeadless()).
# --enable-native-access silences FFM restricted-method warnings from the off-heap GeoEngine.
# shellcheck disable=SC2086
exec java $JAVA_OPTS -Djava.awt.headless=true --enable-native-access=ALL-UNNAMED -cp "$CP" "$MAIN"
