# Arquitectura del motor VFX

## Objetivo

Construir un motor de efectos visuales en tiempo real para Forge 1.20.1 que permita combinar shaders GLSL, geometría, materiales, partículas, estelas, animación temporal y posprocesamiento.

No se da por hecho que todos los efectos puedan resolverse con un solo shader. Los shaders calculan cómo se dibujan los datos que reciben; el mod también crea geometría, gestiona recursos, actualiza animaciones y coordina los efectos con el mundo.

## Capas

### Núcleo común

Responsable de identificadores, definiciones, ciclo de vida, parámetros y estados de los efectos. No debe importar clases exclusivas del cliente como cámaras, matrices de renderizado o buffers gráficos.

### Catálogo central

`EffectRegistry` mantiene las fábricas y metadatos (identificador, nombre visible y descripción). `ClientVfxCatalog` registra los VFX incluidos. Los comandos y las sugerencias se derivan del registro, evitando mantener una lista duplicada en cada sistema.

### Runtime y simulación

`ClientVfxManager` mantiene las instancias activas para efectos visuales locales. Actualiza el estado y llama a las implementaciones de renderizado durante la etapa correcta. El runtime común `EffectManager` sirve como base conceptual para separar la gestión de ciclo de vida de la integración específica del cliente.

Cuando un efecto influya en jugadores, entidades o bloques, esa lógica debe tener una política clara de autoridad del servidor y validación multijugador. La activación por comando del cliente no debe utilizarse para cambiar el mundo de forma autoritativa.

### Renderizado del cliente

`ClientVfxRenderer` ofrece un contrato de renderizado opcional para efectos que dibujan en el mundo. Los renderers deben trabajar con las matrices, la cámara, el estado y las etapas de renderizado que expone Minecraft, sin introducir llamadas gráficas arbitrarias desde el núcleo común.

### Shaders y materiales

`VfxShaderLibrary` registra shaders durante `RegisterShadersEvent` y conserva sus instancias tras la carga de recursos. `VfxRenderTypes` centraliza estados de renderizado reutilizables para que los efectos no tengan que configurar sus propios tipos de dibujo desde cero.

El primer recurso GLSL, `assets/vfxshadersmod/shaders/core/vfx/basic_color`, es un shader de color por vértice: permite validar el recorrido de carga y sirve de punto inicial para geometría personalizada. Cada shader nuevo deberá declarar sus entradas, uniformes, texturas y formato de vértices compatibles con su `RenderType`.

### Posprocesamiento

Los efectos de pantalla deben integrarse en una cadena controlada de renderizado. Bloom, distorsión, desenfoque o cambios de color no se implementan como simples partículas: requieren operar sobre una textura de la escena y respetar el ciclo de renderizado de Minecraft. Esta capa todavía no está implementada.

### Línea de tiempo

Coordina fases y curvas temporales: preparación, aparición, intensidad máxima, transición y salida. Las animaciones deberían poder usar curvas normalizadas y valores reproducibles a partir del tiempo transcurrido.

### Red

El servidor enviará únicamente el estado y los parámetros necesarios para sincronizar eventos que deban verse en otros clientes. Cada cliente realizará su propio renderizado. No se enviarán buffers gráficos ni se confiará al cliente la autoridad de las mecánicas que afectan al mundo.

## Flujo de un efecto

1. El catálogo registra una fábrica, un identificador y sus metadatos.
2. El comando `/vfx enable <id>` crea una instancia e inicializa su estado.
3. El runtime actualiza la simulación en el hilo de juego apropiado.
4. El renderer convierte el estado en geometría, partículas o parámetros de shader.
5. Los componentes de renderizado dibujan usando los estados compartidos.
6. Al terminar o desactivarse, el efecto libera sus recursos y se retira del runtime.

## Reglas para mantener el proyecto sano

- Evitar dependencias cliente en clases comunes para no romper servidores dedicados.
- No realizar operaciones costosas ni asignaciones masivas por fotograma sin medirlas.
- Mantener separados el estado de simulación y los objetos temporales del renderizado.
- Gestionar correctamente recursos gráficos al cambiar de mundo, recargar recursos o cerrar el juego.
- No asumir que una distorsión local de pantalla afecta a otros jugadores.
- Añadir cada nueva etapa detrás de una prueba o demostración pequeña y verificable.

## Estado de la implementación

Implementado: configuración Forge, contratos iniciales, registro con metadatos, runtime local, comandos de control, un VFX de prueba y un shader GLSL de color básico con su tipo de renderizado compartido.

Pendiente: validar visualmente la carga GLSL dentro de Minecraft, agregar más materiales, partículas y estelas avanzadas, crear la cadena de posprocesamiento, definir efectos externos y añadir sincronización de eventos multijugador.
