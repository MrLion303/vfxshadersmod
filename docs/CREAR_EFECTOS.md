# Diseñar y añadir efectos

## Elegir el tipo de efecto

- Ambiental: se extiende por el cielo o el entorno.
- Anclado al mundo: existe en coordenadas concretas y no sigue automáticamente al jugador.
- Ligado a entidad: sigue a un jugador o criatura.
- Pantalla/cámara: modifica la imagen renderizada, como una lente gravitacional.
- Compuesto: combina mallas, materiales, partículas y fases temporales.
- Simulación: cambia el mundo real o aplica fuerzas y requiere autoridad del servidor.

## Contratos principales

- VfxEffect: ciclo de vida de una definición.
- VfxEffectCategory: categoría funcional.
- WorldVfxInstance: aparición colocada con posición y parámetros propios.
- ClientWorldVfxInstances: almacén de apariciones.
- VfxRenderContext: datos de cámara y etapa para renderers espaciales.
- ClientWorldVfxRenderer y ClientWorldVfxRendererRegistry: renderizado de instancias colocadas.
- BlackHoleLensingPass: pase GLSL que deforma la imagen alrededor de agujeros negros.
- EffectRegistry y ClientVfxCatalog: definiciones, metadatos y registro.

## Materiales del renderer propio

Usa el renderizador propio solo cuando el efecto necesite una malla conformada al terreno, una cúpula completa o un shader de pantalla. Para partículas, emisores, rotación orbital, ruido, bloom y estelas, prefiere el runtime real de Photon.

El renderizador centraliza los materiales reutilizables en `VfxRenderTypes`:

- `colorQuads()`: superficies translúcidas con profundidad, para capas base.
- `additiveQuads()`: halos, plasma y partículas que suman luz sin escribir profundidad.
- `skyQuads()`: superficies celestes detrás del mundo.
- `skyAdditiveQuads()`: brillo celeste aditivo.
- `additiveLines()` y `skyAdditiveLines()`: trazos energéticos y grietas con mezcla aditiva.
- Materiales específicos de shader para agujeros negros, portales y nubes procedurales.

Para un efecto con acabado de producción, separa la silueta/base de sus capas de brillo: una capa define la forma, otra más ancha y tenue produce el halo y los detalles finos aportan el núcleo. Evita usar un único quad plano para simular un objeto que deba verse volumétrico. Las animaciones deben derivarse del tiempo del efecto y usar curvas suaves de entrada/salida, evitando saltos al empezar o terminar.

Usa los materiales comunes antes de crear otro RenderType. Conserva las mecánicas de servidor separadas del renderizado del cliente; un material aditivo no sustituye partículas, colisiones ni fuerzas físicas.

## Usar el motor real de Photon

Los efectos de partículas deben preferir el runtime de Photon en vez de escribir un renderer manual de quads para cada partícula. `PhotonEffectBridge` crea un objeto `FX`, agrega `ParticleEmitter` al `mainFX` y lo ejecuta con `FXEffect`/ `FXRuntime`. Para estelas, configura `ParticleConfig.trails`; para órbitas, utiliza `velocityOverLifetime` y la forma `Circle` o `Sphere`.

Al añadir un preset:

1. Agrega su identificador a `PhotonEffectBridge.Preset` y relaciónalo en `playForEffect`.
2. Construye sus emisores con los parámetros de Photon; evita dibujar una segunda copia con un `ClientVfxRenderer`.
3. Marca el efecto con `usesPhotonAsPrimaryRenderer` solo si Photon puede sustituir toda su representación. Si una capa especial sigue siendo necesaria, consulta `ClientVfxManager.isPhotonRunning` para que el renderer propio solo cubra esa capa y no duplique las partículas.
4. Libera el runtime al desactivar el efecto y actualiza el ancla para los efectos que siguen al jugador.

Photon no sustituye automáticamente el posprocesamiento de cámara, una cúpula para tintar todo el cielo ni una mecánica autoritativa del servidor. Esas partes se mantienen separadas hasta que tengan una implementación compatible y probada. En la versión actual, los presets se crean en memoria con la API de Photon; aún no están empaquetados como archivos `.fx` editables directamente desde el editor.

## Crear un efecto colocado

1. Crear una definición con EffectId.
2. Implementar ClientWorldVfxRenderer.
3. Registrar el renderer para su EffectId.
4. Crear una WorldVfxInstance con posición, orientación, escala, duración y parámetros.
5. Utilizar VfxRenderContext para convertir coordenadas mundiales a relativas a cámara.
6. Si el efecto distorsiona el paisaje o el cielo detrás de él, implementar un pase de posprocesamiento y conectarlo en la etapa adecuada.

El comando del agujero negro sirve como ejemplo de tamaño y coordenadas: /vfx enable blackhole <tamaño> <x> <y> <z>. Se desactiva con /vfx disable blackhole.

## Añadir un efecto de cielo

Un efecto que permanezca en una dirección celeste puede implementar VfxEffect y ClientVfxRenderer. Renderiza la geometría en una etapa del cielo, usa coordenadas relativas al observador para evitar paralaje no deseado y define claramente su orientación.

## Añadir un shader GLSL

Los recursos van en src/main/resources/assets/vfxshadersmod/shaders/core/. Crea el JSON de configuración, los archivos vertex/fragment compatibles con el formato de vértices y registra el ShaderInstance en VfxShaderLibrary.

La compilación no garantiza que un shader se vea correctamente en todos los drivers: hay que probarlo dentro de Minecraft, incluidos los cambios de resolución, recarga de recursos y varios efectos simultáneos.


## Cámara cinemática y ráfagas Photon

`VfxCameraCommands` valida los objetivos en el servidor y envía órdenes privadas a cada jugador. `ClientVfxCameraController` realiza el movimiento en el cliente con una entidad auxiliar que nunca se añade al mundo. La vista de hombro usa tercera persona con desplazamiento lateral; la cámara fija usa primera persona para colocar el punto de vista exactamente en las coordenadas solicitadas.

Los tiempos se expresan en segundos o `false` para una transición instantánea. Las direcciones se convierten en yaw/pitch, incluidas diagonales entre un rumbo cardinal y arriba/abajo. `PhotonEffectBridge.playOneShot` retiene las ráfagas temporales de confeti/llamas y destruye sus runtimes tras terminar.
