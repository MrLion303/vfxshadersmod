# Arquitectura del motor VFX

## Objetivo

Construir un motor de efectos visuales en tiempo real para Forge 1.20.1 que combine shaders GLSL, geometría, materiales, partículas, estelas, animación temporal, posprocesamiento y efectos ligados al mundo.

No todos los efectos son efectos de cielo ni todos se resuelven con un único shader. El motor distingue entre geometría del mundo, instancias colocadas en coordenadas, efectos ligados a entidades, simulación de mundo y filtros que modifican la imagen de la cámara.

## Capas

### Núcleo común

Responsable de identificadores, categorías, definición, ciclo de vida y datos de cada instancia. No debe importar clases exclusivas del cliente, como cámaras, matrices de renderizado o buffers gráficos.

VfxEffectCategory separa los tipos de efecto por intención: ambiente, anclado al mundo, ligado a entidad, pantalla/cámara, partículas, simulación del mundo, audio-reactivo y compuesto.

WorldVfxInstance representa una aparición concreta: identificador único de instancia, tipo de efecto, dimensión, posición, rotación, escala, duración y parámetros numéricos. Una misma definición puede tener muchas instancias en distintos lugares. Este objeto describe una aparición visual, no modifica por sí solo bloques ni entidades.

### Catálogo central

EffectRegistry mantiene las fábricas y metadatos (identificador, nombre visible, descripción y categoría). ClientVfxCatalog registra los VFX incluidos. Los comandos y sugerencias se derivan de este registro; no se mantiene una lista paralela en cada sistema.

### Runtime y simulación

ClientVfxManager controla los efectos locales activados con /vfx. ClientWorldVfxInstances administra las apariciones colocadas en el mundo de manera independiente. Ambas capas se actualizan en el hilo del cliente y se limpian al abandonar la sesión.

Un renderer asociado a un tipo puede dibujar muchas apariciones de esa definición. Los efectos globales y los efectos colocados no deben compartir un único estado de activación, porque necesitan ciclos de vida distintos.

Cuando un efecto influya en jugadores, entidades o bloques, esa lógica debe tener una política clara de autoridad del servidor y validación multijugador. La activación por comando del cliente no debe utilizarse para cambiar el mundo de forma autoritativa.

### Renderizado del cliente

ClientVfxRenderer es el contrato sencillo que ya utilizan los efectos globales. VfxRenderContext da a los renderers de mundo una vista común de nivel, cámara, posición de cámara, matrices, etapa y tiempo parcial del fotograma. Incluye una utilidad para convertir coordenadas absolutas del mundo a coordenadas relativas a cámara.

ClientWorldVfxRenderer dibuja una WorldVfxInstance, y ClientWorldVfxRendererRegistry enlaza cada tipo con su renderer y filtra por etapa. Esta ruta permite, por ejemplo, varios fenómenos gravitacionales fijos en diferentes coordenadas sin anclarlos automáticamente al jugador.

### Shaders y materiales

VfxShaderLibrary registra shaders durante RegisterShadersEvent y conserva sus instancias tras la carga de recursos. VfxRenderTypes centraliza estados de renderizado reutilizables.

El recurso GLSL assets/vfxshadersmod/shaders/core/vfx/basic_color es un shader de color por vértice. Sirve como punto de partida para geometría personalizada, pero no implementa por sí solo bloom, distorsión de pantalla ni iluminación volumétrica.

### Posprocesamiento

ClientVfxPostProcessor, VfxPostProcessContext y ClientVfxPostProcessRegistry definen pases ordenables con superficies de entrada y salida separadas. El contexto distingue el origen y el destino para poder organizar un flujo de ping-pong, necesario cuando se encadenan varios filtros.

**Pendiente:** todavía falta conectar el registro con el ciclo de framebuffer de Minecraft, gestionar targets al cambiar la resolución y recargar recursos, e implementar pases concretos como distorsión, gradación de color o bloom. Los contratos no significan que el posprocesamiento ya se ejecute en el juego.

### Línea de tiempo

Coordina fases y curvas temporales: preparación, aparición, intensidad máxima, transición y salida. Las animaciones deberían poder usar curvas normalizadas y valores reproducibles a partir del tiempo transcurrido.

### Red y autoridad

El modelo de instancia lleva identificador de dimensión, posición, rotación, escala y parámetros, datos aptos para construir mensajes de sincronización más adelante. La capa de red aún no está conectada. El servidor deberá autorizar los eventos compartidos; cada cliente hará su propio renderizado. Nunca se enviarán buffers gráficos y el cliente no será la autoridad de mecánicas que afecten al mundo.

## Flujo de un efecto

1. El catálogo registra el tipo de efecto, su categoría, fábrica y metadatos.
2. /vfx enable <id> crea una instancia global para el cliente; las apariciones situadas se gestionan de forma independiente mediante ClientWorldVfxInstances.
3. El runtime actualiza el estado y el tiempo de vida.
4. El renderer transforma estado y coordenadas del mundo en geometría o parámetros de shader.
5. Si se necesita modificar la imagen completa, el efecto se integra como pase de posprocesamiento, no como geometría ubicada en el mundo.
6. Al terminar o abandonar el mundo, el runtime retira las instancias y libera sus recursos.

## Reglas para mantener el proyecto sano

- Evitar dependencias cliente en clases comunes para no romper servidores dedicados.
- No realizar operaciones costosas ni asignaciones masivas por fotograma sin medirlas.
- Mantener separados el estado de simulación y los objetos temporales del renderizado.
- Gestionar correctamente recursos gráficos al cambiar de mundo, recargar recursos o cerrar el juego.
- No asumir que una distorsión local de pantalla también se verá en otros jugadores.
- Diferenciar una deformación puramente visual de una mecánica que realmente modifica bloques.
- Añadir cada nueva etapa detrás de una prueba o demostración pequeña y verificable.

## Estado de la implementación

**Implementado:** configuración Forge, registro con categorías, runtime local, comandos, efectos de prueba, shader GLSL básico, modelo de instancias colocadas, almacén local de apariciones, contexto de renderizado espacial, registro de renderers anclados y contratos/registro ordenado de posprocesadores.

**Pendiente:** asociar un efecto concreto al nuevo renderer de instancias, conectar el pipeline de posprocesamiento al framebuffer, validar el renderizado en el cliente real, implementar materiales y partículas avanzadas y añadir sincronización de eventos multijugador.
