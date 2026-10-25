# Diseño de efectos

## Modelo mental

Un efecto es una composición de componentes, no necesariamente una clase que lo dibuja todo. Un meteorito, por ejemplo, podría combinar:

- un núcleo 3D con material emisivo;
- una estela cuya anchura y transparencia cambian con el tiempo;
- emisores de humo y chispas;
- una secuencia de aparición, vuelo y disipación;
- una explosión y sonido al finalizar, si el diseño lo requiere.

Un agujero negro podría compartir materiales, curvas, emisores y componentes de geometría, aunque su simulación y su posprocesamiento sean propios.

## Contrato inicial

Las clases que implementen `VfxEffect` declaran un `EffectId` y controlan su ciclo de vida mediante `onStart`, `tick`, `isFinished` y `onStop`. No deben hacer llamadas OpenGL directamente desde el contrato común. El renderizado se diseñará con una API de cliente separada.

El registro `EffectRegistry` guarda fábricas para crear instancias nuevas. El `EffectManager` administra instancias activas en un solo hilo; quien lo use será responsable de invocar `tick` en el ciclo adecuado.

## Convenciones previstas

- Identificadores con namespace, por ejemplo `vfxshadersmod:meteor`.
- Parámetros expresados en unidades y escalas documentadas.
- Curvas temporales normalizadas de 0 a 1 cuando sea apropiado.
- Duración y limpieza explícitas; evitar partículas o recursos que queden activos por accidente.
- Separar la simulación que afecta al mundo del efecto puramente visual.
- Mantener un comportamiento alternativo cuando una opción gráfica avanzada no esté disponible.

## Definiciones externas

Más adelante, los efectos compuestos sencillos podrán describirse mediante JSON: componentes, materiales, parámetros, curvas, referencias a texturas y secuencias. Las características que requieren lógica propia seguirán implementándose en Java.

No conviene diseñar el formato definitivo antes de tener varios componentes reales. Primero se validará el motor con pequeños prototipos y después se fijará el esquema de datos.
