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
- `ClientVfxRenderer`: interfaz opcional para dibujar en el cliente en una etapa del renderizado.
- `EffectRegistry`: registro de fábricas; el listado de comandos se construye a partir de él.
- `ClientVfxManager`: instancias activas, actualización, renderizado y limpieza.

El contrato común no debe llamar a OpenGL ni importar clases exclusivas del cliente. Un efecto visual local puede ejecutarse en el cliente; si modifica jugadores, criaturas o bloques, la mecánica debe diseñarse con autoridad del servidor y sincronización explícita.

## Añadir un efecto

1. Crear una clase que implemente `VfxEffect` y definir un `EffectId` único.
2. Si dibuja en pantalla o en el mundo, implementar `ClientVfxRenderer` o conectar un renderer propio del cliente.
3. Añadir la fábrica a `ClientVfxCatalog.registerDefaults()`.
4. Probar que aparece en `/vfx list` y que funciona con `/vfx enable <id>`, `/vfx disable <id>` y `/vfx toggle <id>`.
5. Actualizar esta documentación con el comportamiento, parámetros y limitaciones del efecto.

Ejemplo de identificador:

```java
public static final EffectId ID =
        EffectId.of(VfxShadersMod.MOD_ID, "meteor");
```

La lista de comandos, las sugerencias de autocompletado y el estado activo se derivan del registro y del runtime; no es necesario mantener una lista de IDs en el archivo de comandos.

## Convenciones previstas para los materiales

Cuando el motor de materiales esté implementado:

- Cada material declarará el shader y las texturas que usa.
- Los parámetros serán explícitos y documentados (color, emisión, opacidad, distorsión, desplazamiento).
- Las fases temporales emplearán curvas normalizadas de 0 a 1 cuando sea apropiado.
- Los recursos gráficos se liberarán al descargar el mundo o recargar los recursos.
- Se dará prioridad a composición y reutilización en lugar de crear una clase enorme para cada escena.

## Definiciones externas

Más adelante, los efectos compuestos sencillos podrán describirse mediante JSON: componentes, materiales, parámetros, curvas, referencias a texturas y secuencias. Los comportamientos que requieren lógica propia seguirán implementándose en Java. El formato definitivo debe diseñarse después de validar varios componentes reales, para no comprometer el proyecto con un esquema que todavía no se ha probado.
