# Arquitectura del motor VFX

## Objetivo

Construir un motor de efectos visuales en tiempo real para Forge 1.20.1 que combine shaders GLSL, geometría, materiales, partículas, estelas, animación temporal, posprocesamiento y efectos ligados al mundo.

No todos los efectos son efectos de cielo ni todos se resuelven con un solo shader. El motor distingue entre geometría del mundo, instancias colocadas en coordenadas, efectos ligados a entidades, simulación de mundo y filtros que modifican la imagen de la cámara.

## Capas

### Núcleo y catálogo

VfxEffectCategory clasifica los efectos según su papel: ambiente, anclado al mundo, ligado a entidad, pantalla/cámara, partículas, simulación del mundo, audio-reactivo o compuesto. EffectRegistry mantiene fábricas, metadatos y categorías; ClientVfxCatalog registra los VFX incluidos.

WorldVfxInstance representa una aparición concreta: identificador único, tipo de efecto, dimensión, posición, rotación, escala, duración y parámetros. Una misma definición puede tener muchas instancias en distintos lugares. Esta estructura describe una aparición visual; no modifica por sí sola bloques ni entidades.

### Runtime

ClientVfxManager controla los efectos locales activados con /vfx. ClientWorldVfxInstances administra las apariciones colocadas en el mundo de manera independiente. Ambas capas se actualizan en el hilo del cliente y se limpian al abandonar la sesión.

Un renderer asociado a un tipo puede dibujar varias apariciones de esa definición. Los efectos globales y los efectos colocados no comparten un único estado de activación.

Cuando un efecto influya en jugadores, entidades o bloques reales, esa lógica debe tener autoridad del servidor y validación multijugador. La activación por comando del cliente no cambia el mundo de forma autoritativa.

### Renderizado espacial

ClientVfxRenderer es el contrato de renderizado para efectos globales. VfxRenderContext da a los renderers de mundo una vista común de nivel, cámara, posición de cámara, matrices, etapa y tiempo parcial del fotograma.

ClientWorldVfxRenderer dibuja una WorldVfxInstance y ClientWorldVfxRendererRegistry enlaza cada tipo con su renderer y filtra por etapa. Esto permite varios fenómenos gravitacionales fijos en coordenadas diferentes sin anclarlos automáticamente al jugador.

### Shaders y materiales

VfxShaderLibrary registra shaders durante RegisterShadersEvent y conserva sus instancias tras la carga de recursos. VfxRenderTypes centraliza estados reutilizables para geometría translúcida y opaca.

El shader basic_color es un shader de color por vértice. El shader black_hole_lensing usa la textura renderizada del mundo y coordenadas de pantalla para deformar la imagen alrededor de los agujeros negros proyectados.

### Posprocesamiento y lente gravitacional

BlackHoleLensingPass se ejecuta después del renderizado del nivel. Ordena las instancias visibles por distancia, proyecta sus posiciones y radios a coordenadas de pantalla y procesa hasta ocho agujeros negros visibles en un pase GLSL. La textura original se dibuja en un target auxiliar y el resultado se copia al target principal para no muestrear y escribir en la misma textura a la vez.

El pase de lente curva la imagen de bloques y paisaje alrededor del agujero. **Es una deformación visual de pantalla**: no cambia la geometría del mundo, no destruye bloques ni aplica fuerzas. Otros efectos de pantalla necesitarán sus propios pases.

### Línea de tiempo

Los efectos usan el tiempo de juego y el tiempo parcial de fotograma para animar geometría y color. Las próximas etapas permitirán fases, curvas temporales y transiciones más complejas.

### Red y autoridad

La capa de red todavía no está conectada. El servidor deberá autorizar los eventos visuales compartidos; cada cliente hará su propio renderizado. Nunca se enviarán buffers gráficos y el cliente no será la autoridad de mecánicas que afecten al mundo.

## Flujo de un efecto

1. El catálogo registra el tipo de efecto, su categoría, fábrica y metadatos.
2. /vfx enable <id> activa el efecto global; las apariciones colocadas se crean de forma independiente.
3. El runtime actualiza el estado y el tiempo de vida.
4. El renderer transforma el estado y las coordenadas del mundo en geometría.
5. Los pases de pantalla se ejecutan tras el renderizado de nivel y procesan la imagen renderizada.
6. Al terminar o abandonar el mundo, el runtime retira las instancias y libera los targets auxiliares.

## Reglas para mantener el proyecto sano

- Evitar dependencias cliente en clases comunes para no romper servidores dedicados.
- No realizar operaciones costosas ni asignaciones masivas por fotograma sin medirlas.
- Mantener separados el estado de simulación y los objetos temporales del renderizado.
- Gestionar correctamente recursos gráficos al cambiar de mundo, recargar recursos o cerrar el juego.
- No asumir que una distorsión local de pantalla también se verá en otros jugadores.
- Diferenciar una deformación puramente visual de una mecánica que realmente modifica bloques.
- Añadir cada nueva etapa detrás de una prueba o demostración pequeña y verificable.

## Estado de la implementación

Implementado: registro y categorías, runtime local, comandos, efectos de prueba, shaders GLSL, instancias colocadas, registro de renderers espaciales, cielo arcoíris cambiante, anillos continuos y agujero negro configurable con pase de lente gravitacional.

Pendiente: pruebas visuales en cliente real, validación de rendimiento y compatibilidad del pase con distintas configuraciones gráficas, materiales más generales, partículas y estelas avanzadas y sincronización multijugador.
