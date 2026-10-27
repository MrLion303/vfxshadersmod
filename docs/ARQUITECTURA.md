# Arquitectura del motor VFX

## Catálogo y runtime

`EffectRegistry` guarda identificadores, categorías y fábricas de efectos. `ClientVfxManager` ejecuta los efectos en el hilo gráfico del cliente. Las apariciones colocadas en el mundo viven en `WorldVfxInstance`, con dimensión, coordenadas, escala, duración y parámetros.

## Composición visual y materiales

Los VFX comparten materiales de renderizado en `VfxRenderTypes`. Las superficies base conservan profundidad cuando corresponde; los halos, estelas, grietas y partículas usan mezcla aditiva sin escribir profundidad. Los efectos de cielo tienen variantes que omiten la prueba de profundidad para permanecer detrás del mundo. Los efectos complejos se construyen por capas en lugar de depender de una sola superficie: base, volumen/estructura y brillo.

Los materiales comunes están integrados en lluvia de estrellas, aurora del suelo, escaneo del terreno, aurora arcoíris, vórtice, portal, órbitas de demostración y estelas de Starsspeed. Los shaders especializados de agujero negro, disco de acreción, portal y nube procedural se mantienen como materiales específicos porque controlan geometría o distorsión que un material genérico no puede sustituir.

## Agujeros negros

`blackhole` se ancla a coordenadas absolutas del mundo y tiene tamaño en bloques. El renderer dibuja una sombra negra, un disco de acreción cálido casi horizontal y halos. `BlackHoleLensingPass` calcula centros y radios aparentes con los vectores de orientación de la cámara, proyecta las ubicaciones y modifica la textura de la escena con un shader GLSL. El shader crea una deformación fuerte y amplia del fondo, una región central negra y una banda luminosa blanco-dorada. Esta representación es una aproximación cinematográfica; no es una simulación física relativista.

`blackhole_sky` utiliza una dirección fija de mundo a 50° sobre el horizonte (cenit = 90°), en lugar de capturar la dirección de mirada al activarse. Su escala es mucho mayor para poder funcionar como evento de cielo desde lejos.

## Cielo arcoíris

`rainbow_sky` utiliza una cúpula que cubre el cielo entero hasta el cenit. El color se mantiene uniforme por etapa: azul, rojo, naranja, amarillo, verde, cian, índigo y violeta, con transiciones suaves entre tonos. El pase de pantalla usa la textura de profundidad para teñir el cielo y las nubes mientras conserva los bloques cercanos.

## Comandos y red

Los comandos escritos en el cliente pueden controlar los efectos locales. `VfxServerCommands` registra también `/vfx` en el servidor para ejecución por bloques de comandos. `VfxNetwork` y `VfxCommandPacket` envían las órdenes de activación, desactivación y colocación a los clientes de la misma dimensión; `ClientVfxNetworkHandler` aplica la orden en el hilo del cliente.

`/vfxmove empujon` es mecánico y se ejecuta en servidor. Conserva la componente horizontal solicitada y usa una componente vertical más alta para una trayectoria parabólica más pronunciada.

## Estado y limitaciones

La compilación valida Java y recursos, pero no sustituye la prueba dentro del cliente. El resultado visual del shader puede variar por drivers y configuración gráfica. La lente es de pantalla, no modifica bloques ni simula de forma física el espacio-tiempo. El comando de eventos visuales requiere el mod en servidor y clientes para que todos vean el mismo VFX.
