# Arquitectura del motor VFX

## Catálogo y runtime

`EffectRegistry` guarda identificadores, categorías y fábricas de efectos. `ClientVfxManager` ejecuta los efectos en el hilo gráfico del cliente. Las apariciones colocadas en el mundo viven en `WorldVfxInstance`, con dimensión, coordenadas, escala, duración y parámetros.

## Integración con el runtime real de Photon

`PhotonEffectBridge` construye efectos con las clases oficiales `FX`, `FXEffect`, `ParticleEmitter` y `ParticleConfig` de Photon 1.1.17 para Forge 1.20.1. Los emisores, las formas, el movimiento orbital, el bloom y las estelas se procesan en el runtime de Photon; no es una imitación con un renderer propio.

La migración se mantiene compuesta para no perder mecánicas que Photon no reemplaza por sí solo. `demo_orbit`, `music_horizon_bars`, `rainbow_aurora`, `cloud_vortex` y `falling_stars` usan Photon como renderer principal y no dibujan también sus versiones antiguas. En `starsspeed`, Photon dibuja las estrellas y sus estelas; el renderer propio conserva únicamente el tinte global del cielo.

Las ondas de `ground_aurora`, `scan` y `trampolin` usan emisores Photon con curvas de escala, además de conservar las superficies que siguen el relieve y la mecánica del servidor. El portal y los agujeros negros combinan anillos, bloom y estelas Photon con shaders propios de interior o lente de pantalla; el tinte de `rainbow_sky` también se mantiene como shader porque Photon no reemplaza una cúpula de color para todo el cielo.

Los presets se construyen en memoria mediante la API real de Photon; todavía no están empaquetados como archivos `.fx` editables directamente desde el editor. La dependencia se resuelve desde el artefacto de Photon para Forge 1.20.1 junto con LowDragLib, sin copiar el código fuente de Photon.

## Composición visual y materiales

Los VFX comparten materiales de renderizado en `VfxRenderTypes`. Las superficies base conservan profundidad cuando corresponde; los halos, estelas, grietas y partículas usan mezcla aditiva sin escribir profundidad. Los efectos de cielo tienen variantes que omiten la prueba de profundidad para permanecer detrás del mundo. Los efectos complejos se construyen por capas en lugar de depender de una sola superficie: base, volumen/estructura y brillo.

Los `RenderType` propios se reservan para las capas que requieren una superficie adaptada a los bloques, un cielo completo o un pase GLSL de pantalla. Las partículas, los emisores, el movimiento orbital, el ruido, el bloom y las estelas de los efectos migrados los procesa Photon. Los shaders de agujero negro, cielo arcoíris y portal se mantienen porque realizan distorsiones o cambios de escena que el sistema de partículas no sustituye.

## Agujeros negros

`blackhole` se ancla a coordenadas absolutas del mundo y tiene tamaño en bloques. El renderer dibuja una sombra negra, un disco de acreción cálido casi horizontal y halos. `BlackHoleLensingPass` calcula centros y radios aparentes con los vectores de orientación de la cámara, proyecta las ubicaciones y modifica la textura de la escena con un shader GLSL. El shader crea una deformación fuerte y amplia del fondo, una región central negra y una banda luminosa blanco-dorada. Esta representación es una aproximación cinematográfica; no es una simulación física relativista.

`blackhole_sky` utiliza una dirección fija de mundo, en lugar de capturar la dirección de mirada al activarse. El ancla y la escala del disco de partículas Photon coinciden con el disco del renderer celeste; los agujeros negros colocados mantienen un preset independiente y casi horizontal.

`sky_portal` combina su ventana galáctica especializada con anillos Photon cuyo radio crece durante el mismo intervalo de apertura. El ancla Photon usa el mismo centro celeste que el renderer del portal, para evitar que las partículas aparezcan separadas de la ventana.

## Cielo arcoíris

`rainbow_sky` utiliza una cúpula que cubre el cielo entero hasta el cenit. El color se mantiene uniforme por etapa: azul, rojo, naranja, amarillo, verde, cian, índigo y violeta, con transiciones suaves entre tonos. El pase de pantalla usa la textura de profundidad para teñir el cielo y las nubes mientras conserva los bloques cercanos.

## Comandos y red

Los comandos escritos en el cliente pueden controlar los efectos locales. `VfxServerCommands` registra también `/vfx` en el servidor para ejecución por bloques de comandos. `VfxNetwork` y `VfxCommandPacket` envían las órdenes de activación, desactivación y colocación a los clientes de la misma dimensión; `ClientVfxNetworkHandler` aplica la orden en el hilo del cliente.

`/vfxmove empujon` es mecánico y se ejecuta en servidor. Conserva la componente horizontal solicitada y usa una componente vertical más alta para una trayectoria parabólica más pronunciada.

## Estado y limitaciones

La compilación valida Java y recursos, pero no sustituye la prueba dentro del cliente. El resultado visual del shader puede variar por drivers y configuración gráfica. La lente es de pantalla, no modifica bloques ni simula de forma física el espacio-tiempo. El comando de eventos visuales requiere el mod en servidor y clientes para que todos vean el mismo VFX.
