# Premium Client

Mod de cliente para Minecraft 26.2 con Fabric.

## Comandos

- `*premium`: muestra la ayuda.
- `*premium get`: lista en el chat local los jugadores conectados cuyo UUID es v4.
- `*premium check <usuario>`: comprueba si un jugador conectado tiene un UUID v4. Pulsa Tab para autocompletar comandos y nombres.

El comando se intercepta antes de enviar el texto, así que no llega al servidor ni a otros jugadores. La lista procede únicamente de los jugadores que el servidor muestra a tu cliente. UUID v4 es una estimación de cuenta autenticada, no una prueba: algunos servidores o proxies pueden cambiar la clasificación.

## Compilar e instalar

Ejecuta `gradle build` desde esta carpeta. Copia `build/libs/premium-client-1.0.0.jar` a la carpeta `mods` de tu instalación de Fabric 26.2. Fabric API también debe estar instalado.