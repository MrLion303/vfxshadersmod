# Diseñar y añadir efectos

## Modelo mental

Un efecto es una composición de componentes, no necesariamente una clase que lo dibuja todo. Un meteorito, por ejemplo, podría combinar:

- un núcleo 3D con material emisivo;
- una estela cuya anchura y transparencia cambian con el tiempo;
- emisores de humo y chispas;
- una secuencia de aparición, vuelo y disipación;
- una explosión y un sonido al finalizar, si el diseño lo requiere.

Un agujero negro podría compartir materiales, curvas, emisores y componentes geométricos, pero también necesitar un disco de acreción, una instancia fija en el mundo y un pase de distorsión de pantalla.

## Elegir dónde vive un efecto

- **Ambiental:** se extiende por una zona amplia o forma parte del entorno, como auroras y fenómenos del cielo.
- **Anclado al mundo:** tiene coordenadas concretas y no sigue automáticamente al jugador. Puede existir más de una instancia del mismo tipo.
- **Ligado a entidad:** sigue a un jugador, criatura u otra entidad.
- **Pantalla/cámara:** procesa la imagen que ve la cámara; por ejemplo, aberración cromática o lente gravitacional.
- **Partículas o compuesto:** combina emisores, geometría, materiales y fases.
- **Simulación del mundo:** altera el mundo real o aplica fuerzas. Requiere reglas de autoridad y validación del servidor; no es lo mismo que deformar visualmente la imagen de un bloque.

## Contratos de código

- VfxEffect: identificador, inicio, actualización, finalización y limpieza.
- VfxEffectCategory: categoría funcional que queda guardada en el registro.
- WorldVfxInstance: una aparición concreta con dimensión, posición, rotación, escala, duración y parámetros.
- ClientWorldVfxInstances: almacén local para múltiples apariciones colocadas.
- VfxRenderContext: datos compartidos de cámara y etapa para un renderer del mundo.
- ClientWorldVfxRenderer: dibuja una aparición concreta.
- ClientWorldVfxRendererRegistry: enlaza los tipos colocados a sus renderers.
- ClientVfxPostProcessor y VfxPostProcessContext: contrato de un pase de pantalla con entrada y salida separadas.
- EffectRegistry: catálogo de fábricas, categorías y metadatos.
- ClientVfxManager: ciclo de vida de los efectos globales activados mediante comandos.
- VfxShaderLibrary y VfxRenderTypes: infraestructura existente de GLSL y estados compartidos.

El almacén colocado y los contratos de posprocesamiento son la base de extensión. Todavía no se ha conectado un pase de posprocesamiento al framebuffer ni se ha registrado un efecto de ejemplo en el sistema de instancias.

## Añadir un efecto global

1. Crear una clase que implemente VfxEffect y definir un EffectId único, por ejemplo vfxshadersmod:meteor.
2. Si dibuja durante el renderizado del mundo, implementar ClientVfxRenderer.
3. Registrarlo en ClientVfxCatalog.registerDefaults() con la categoría adecuada.
4. Comprobar que aparece en /vfx list y funciona con los comandos de activación.
5. Actualizar esta documentación con el comportamiento y las limitaciones.

## Añadir un efecto colocado

1. Crear una definición y un renderer que implemente ClientWorldVfxRenderer.
2. En renderStages(), devolver solo las etapas en las que se debe dibujar.
3. Registrar el renderer en ClientWorldVfxRendererRegistry con el EffectId de la definición.
4. Crear apariciones con ClientWorldVfxInstances.spawn(...), proporcionando el nivel del cliente, posición absoluta, rotación en grados, escala, duración en ticks y parámetros.
5. En cada llamada a render, usar VfxRenderContext.toCameraRelative(...) para pasar coordenadas del mundo a coordenadas relativas a cámara.
6. Probar varias instancias del mismo tipo a diferentes distancias, al cambiar de dimensión y al abandonar el mundo.

La API de instancias aún no tiene un comando /vfx place, y no se ha añadido sincronización de red. No hay que considerar esas funciones terminadas hasta implementarlas y probarlas.

## Añadir un shader GLSL

Los recursos GLSL van en src/main/resources/assets/vfxshadersmod/shaders/core/. El shader base actual vive en vfx/basic_color.json, vfx/basic_color.vsh y vfx/basic_color.fsh.

Para añadir otro shader:

1. Crear el JSON de configuración y los archivos vertex/fragment shader compatibles con el formato de vértices.
2. Registrar la instancia en VfxShaderLibrary mediante RegisterShadersEvent, guardando la instancia que entrega el callback.
3. Si requiere un nuevo formato o estado de dibujo, añadir un tipo reutilizable en VfxRenderTypes.
4. Usar el tipo de renderizado desde un efecto y probarlo en una sesión real de Minecraft; que el código compile no garantiza que el driver cargue el GLSL correctamente.

El shader basic_color solo transporta y multiplica el color por vértice. No es todavía un sistema de bloom, distorsión de pantalla ni iluminación volumétrica.

## Posprocesamiento

Un pase debe implementar ClientVfxPostProcessor, declarar un identificador estable y una prioridad, y procesar un VfxPostProcessContext con fuentes y destinos distintos. El orden se obtiene de ClientVfxPostProcessRegistry.processors().

Esta API todavía es solo el contrato de extensión: el hook de framebuffer, la administración de targets, el manejo de cambios de resolución y los shaders de pantalla deben implementarse antes de que los filtros se ejecuten en el juego.

## Convenciones para materiales

- Cada material declarará el shader y las texturas que utiliza.
- Los parámetros serán explícitos y documentados (color, emisión, opacidad, distorsión, desplazamiento).
- Las fases temporales emplearán curvas normalizadas de 0 a 1 cuando sea apropiado.
- Los recursos gráficos se liberarán al descargar el mundo o recargar los recursos.
- Se dará prioridad a composición y reutilización en lugar de crear una clase enorme por escena.

## Definiciones externas

Más adelante, los efectos compuestos sencillos podrán describirse mediante JSON: componentes, materiales, parámetros, curvas, referencias a texturas y secuencias. Los comportamientos que requieren lógica propia seguirán implementándose en Java.
