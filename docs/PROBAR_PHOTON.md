# Probar la migración a Photon

Esta guía corresponde al mod para **Minecraft 1.20.1 + Forge 47.4.18**. La integración usa el runtime de partículas de Photon 1.1.17; sus emisores se crean con la API real de Photon dentro del mod.

## Instalación

1. Usa una instancia de Minecraft Java **1.20.1 con Forge 47.4.18 o compatible**.
2. Instala **Photon 1.1.17 para Forge 1.20.1** y las dependencias que pide esa versión de Photon, incluida LowDragLib si tu instalador no la agregó automáticamente.
3. Descarga el JAR del artefacto **Compilar el mod** de GitHub Actions correspondiente al commit que quieres probar.
4. Copia el JAR del mod a la carpeta `mods`. No extraigas ni combines los JAR.
5. Inicia el juego. Si pruebas en un servidor dedicado, instala las dependencias requeridas en el servidor y en los clientes; usa las mismas versiones.

## Prueba sugerida

Entra a un mundo de prueba, activa un efecto a la vez y revisa su posición, movimiento, desaparición y posibles errores en `logs/latest.log`.

### Efectos que usan Photon como renderizador principal

```text
/vfx enable demo_orbit
/vfx disable demo_orbit
/vfx enable rainbow_aurora_photon
/vfx disable rainbow_aurora_photon
/vfx enable falling_stars
/vfx disable falling_stars
```

### Efectos clásicos y compuestos

Las barras musicales, la aurora clásica y Starsspeed usan sus renderizadores originales. El vórtice combina la textura de referencia con partículas Photon.

```text
/vfx enable music_horizon_bars
/vfx disable music_horizon_bars
/vfx enable rainbow_aurora
/vfx disable rainbow_aurora
/vfx enable starsspeed blue
/vfx disable starsspeed
/vfx enable cloud_vortex
/vfx disable cloud_vortex
```

### Efectos compuestos

Estos combinan partículas Photon con una capa especializada del mod cuando se necesita teñir todo el cielo, deformar la escena, seguir el relieve o conservar la física del servidor.

```text
/vfx enable starsspeed blue
/vfx disable starsspeed
/vfx enable ground_aurora ~ ~ ~ blue
/vfx disable ground_aurora
/vfxonce scan ~ ~ ~ blue
/vfxonce trampolin ~ ~ ~ 12 32 0.65
/vfx enable sky_portal
/vfx disable sky_portal
/vfx enable blackhole_sky
/vfx disable blackhole_sky
/vfx enable rainbow_sky
/vfx disable rainbow_sky
/vfx enable blackhole 10 ~ ~ ~
/vfx disable blackhole
```

Los comandos `/vfxonce` y la colocación del agujero negro requieren permisos de operador cuando se ejecutan del lado del servidor. El tamaño del agujero negro es su diámetro en bloques. Si un comando con `~` no acepta coordenadas relativas en tu contexto, sustituye `~ ~ ~` por tres coordenadas numéricas.

Para limpiar los efectos locales antes de seguir probando:

```text
/vfx disable all
```

## Qué verificar

- Las órbitas, la lluvia de estrellas y `rainbow_aurora_photon` deben verse una sola vez. La aurora clásica, las barras musicales y Starsspeed deben usar sus renderizadores originales; el vórtice combina textura de referencia y partículas Photon.
- Las bandas de la aurora y los anillos del portal deben conservar un movimiento continuo y no saltar a una posición distinta del efecto principal.
- Las ondas de escaneo, aurora del suelo y trampolín deben seguir la superficie del terreno mediante su capa especializada. El trampolín también debe lanzar a los jugadores cuando el frente los alcanza.
- Los discos Photon del agujero negro colocado y del agujero negro celeste deben compartir el plano de sus respectivos discos visuales.
- En `trampolin`, comprueba que se vea el frente cian y que las crestas sobresalgan del terreno; la altura depende de la configuración de potencia.
- La lente de pantalla del agujero negro colocado queda desactivada temporalmente para evitar el fallo de pantalla negra; el horizonte y el disco de acreción siguen dibujándose.
- Activa efectos individualmente antes de probar varios a la vez para detectar cualquier caída de rendimiento.

La compilación automática valida Java, recursos JSON y shaders GLSL, pero no puede confirmar el resultado visual de la GPU. La verificación final de aspecto y rendimiento requiere abrir Minecraft con Photon instalado. Los presets se generan mediante la API de Photon en tiempo de ejecución; no están empaquetados como archivos `.fx` editables en el editor de Photon.
