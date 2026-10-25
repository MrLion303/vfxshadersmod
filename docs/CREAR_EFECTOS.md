# Diseñar y añadir efectos

## Modelo mental

Un efecto es una composición de componentes, no necesariamente una clase que lo dibuja todo. Un meteorito podría combinar un núcleo 3D con material emisivo, una estela, emisores de humo y chispas, y una secuencia temporal de aparición, vuelo y disipación.

Un agujero negro requiere una definición de efecto, una aparición colocada en coordenadas, una malla para su horizonte y disco de acreción y un pase de pantalla que deforme la escena en torno a la posición proyectada.

## Elegir dónde vive un efecto

- Ambiental: se extiende por una zona amplia o forma parte del entorno, como auroras y fenómenos del cielo.
- Anclado al mundo: tiene coordenadas concretas y no sigue automáticamente al jugador. Puede haber varias instancias del mismo tipo.
- Ligado a entidad: sigue a un jugador, criatura u otra entidad.
- Pantalla/cámara: procesa la imagen que ve la cámara; por ejemplo, una lente gravitacional.
- Partículas o compuesto: combina emisores, geometría, materiales y fases.
- Simulación del mundo: altera el mundo real o aplica fuerzas. Requiere reglas de autoridad y validación del servidor; no es lo mismo que deformar visualmente la imagen de un bloque.

## Contratos principales

- VfxEffect: identificador, inicio, actualización, finalización y limpieza.
- VfxEffectCategory: categoría funcional guardada en el registro.
- WorldVfxInstance: aparición concreta con dimensión, posición, rotación, escala, duración y parámetros.
- ClientWorldVfxInstances: almacén local para múltiples apariciones colocadas.
- VfxRenderContext: datos compartidos de cámara y etapa para un renderer espacial.
- ClientWorldVfxRenderer y ClientWorldVfxRendererRegistry: dibujan y registran apariciones ancladas al mundo.
- BlackHoleLensingPass: pase GLSL que proyecta agujeros negros en pantalla y deforma la textura del nivel.
- EffectRegistry: catálogo de fábricas, categorías y metadatos.
- ClientVfxManager: ciclo de vida de efectos globales activados mediante comandos.

## Añadir un efecto global

1. Crear una clase que implemente VfxEffect y definir un EffectId único.
2. Si dibuja durante el renderizado del mundo, implementar ClientVfxRenderer.
3. Registrarlo en ClientVfxCatalog.registerDefaults() con la categoría adecuada.
4. Comprobar que aparece en /vfx list y funciona con los comandos de activación.
5. Actualizar esta documentación con el comportamiento y las limitaciones.

## Añadir un efecto colocado

1. Crear una definición y un renderer que implemente ClientWorldVfxRenderer.
2. En renderStages(), devolver solo las etapas en las que se debe dibujar.
3. Registrar el renderer en ClientWorldVfxRendererRegistry con el EffectId de la definición.
4. Crear apariciones con ClientWorldVfxInstances.spawn(...), indicando nivel del cliente, posición absoluta, rotación, escala, duración y parámetros.
5. Utilizar VfxRenderContext.toCameraRelative(...) al emitir vértices relativos a la cámara.
6. Probar varias instancias del mismo tipo a diferentes distancias, al cambiar de dimensión y al abandonar el mundo.

La API de instancias está conectada al comando del agujero negro como ejemplo. Otros tipos pueden reutilizarla. La sincronización de red aún no está añadida.

## Añadir un shader GLSL

Los recursos GLSL van en src/main/resources/assets/vfxshadersmod/shaders/core/. Cada shader debe tener configuración JSON y archivos vertex/fragment compatibles con su formato de vértices. Regístralo en VfxShaderLibrary mediante RegisterShadersEvent y almacena la instancia que entrega el callback.

El shader basic_color solo transporta el color por vértice. El shader black_hole_lensing trabaja con la textura de pantalla y los parámetros proyectados de los agujeros negros. El runtime debe renderizar a una textura auxiliar distinta antes de copiar el resultado al target principal.

## Limitaciones y pruebas

La lente gravitacional actual deforma la imagen en pantalla. No altera la geometría real de los bloques ni modifica las mecánicas del mundo. Antes de considerar terminado un efecto, compílalo, pruébalo en una sesión real y verifica el comportamiento ante recargas de recursos, cambios de resolución, varios efectos simultáneos y equipos de rendimiento limitado.
