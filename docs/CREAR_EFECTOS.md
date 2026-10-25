# Diseñar y añadir efectos

## Modelo mental

Un efecto es una composición de componentes, no necesariamente una clase que lo dibuja todo. Un meteorito, por ejemplo, podría combinar:

- un núcleo 3D con material emisivo;
- una estela cuya anchura y transparencia cambian con el tiempo;
- emisores de humo y chispas;
- una secuencia de aparición, vuelo y disipación;
- una explosión y un sonido al finalizar, si el diseño lo requiere.

Un agujero negro podría compartir materiales, curvas, emisores y componentes geométricos, aunque su simulación y su posprocesamiento sean propios.

## Contratos de código

- `VfxEffect`: identificador, inicio, actualización, finalización y limpieza.
- `EffectContext`: nivel, tiempo de juego y fuente de aleatoriedad para la simulación.
- `ClientVfxRenderer`: interfaz opcional para dibujar en el cliente durante una etapa del renderizado.
- `EffectRegistry`: registro de fábricas y metadatos; alimenta la lista y el autocompletado de comandos.
- `ClientVfxCatalog`: punto central donde se registran los VFX que vienen incluidos.
- `ClientVfxManager`: instancias activas, actualización, renderizado y limpieza.
- `VfxShaderLibrary`: carga de recursos GLSL del mod.
- `VfxRenderTypes`: tipos de renderizado compartidos.

El contrato común no debe llamar a OpenGL ni importar clases exclusivas del cliente. Un efecto visual local puede ejecutarse en el cliente; si modifica jugadores, criaturas o bloques, la mecánica debe diseñarse con autoridad del servidor y sincronización explícita.

## Añadir un efecto nuevo

1. Crear una clase que implemente `VfxEffect` y definir un `EffectId` único, por ejemplo `vfxshadersmod:meteor`.
2. Si necesita dibujar en el mundo, implementar `ClientVfxRenderer` o conectar un renderer asociado del lado cliente.
3. Añadir la fábrica y sus metadatos en `ClientVfxCatalog.registerDefaults()`.
4. Probar que aparece en `/vfx list` y funciona con `/vfx enable <id>`, `/vfx disable <id>`, `/vfx toggle <id>` y `/vfx status <id>`.
5. Actualizar esta documentación con el comportamiento, parámetros y limitaciones.

No hay que editar la lista de comandos. Se construye a partir de `EffectRegistry`, con el nombre, la descripción y el estado activo.

## Añadir un shader GLSL

Los recursos GLSL van en `src/main/resources/assets/vfxshadersmod/shaders/core/`. El shader base actual vive en `vfx/basic_color.json`, `vfx/basic_color.vsh` y `vfx/basic_color.fsh`.

Para añadir otro shader:

1. Crear el JSON de configuración y los archivos vertex/fragment shader compatibles con el formato de vértices.
2. Registrar la instancia en `VfxShaderLibrary` mediante `RegisterShadersEvent`, guardando la instancia que entrega el callback.
3. Si requiere un nuevo formato o estado de dibujo, añadir un tipo reutilizable en `VfxRenderTypes`.
4. Usar el tipo de renderizado desde un efecto y probarlo en una sesión real de Minecraft; que el código compile no garantiza que el driver cargue el GLSL correctamente.

El shader `basic_color` solo transporta y multiplica el color por vértice. No es todavía un sistema de bloom, distorsión de pantalla ni iluminación volumétrica.

## Convenciones previstas para materiales

- Cada material declarará el shader y las texturas que utiliza.
- Los parámetros serán explícitos y documentados (color, emisión, opacidad, distorsión, desplazamiento).
- Las fases temporales emplearán curvas normalizadas de 0 a 1 cuando sea apropiado.
- Los recursos gráficos se liberarán al descargar el mundo o recargar los recursos.
- Se dará prioridad a composición y reutilización en lugar de crear una clase enorme por escena.

## Definiciones externas

Más adelante, los efectos compuestos sencillos podrán describirse mediante JSON: componentes, materiales, parámetros, curvas, referencias a texturas y secuencias. Los comportamientos que requieren lógica propia seguirán implementándose en Java. El formato definitivo se diseñará después de validar varios componentes reales.
