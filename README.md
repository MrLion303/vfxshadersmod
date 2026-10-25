# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a **Forge 1.20.1**.

El objetivo no es añadir solamente una lista de efectos prefabricados. Queremos construir una base extensible para combinar shaders, geometría personalizada, partículas, animaciones, posprocesamiento y comportamiento del mundo. La meta es poder crear escenas cinematográficas —meteoritos con humo, portales, anomalías gravitacionales, supernovas, cielos dinámicos y efectos nuevos— sin tener que rehacer el sistema desde cero para cada una.

> **Estado:** etapa inicial de estructura y arquitectura. El proyecto todavía no contiene los efectos cinematográficos terminados ni un editor visual.

## Plataforma

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

## Principios del proyecto

- **Independiente:** no modifica ni depende del repositorio `shadermod`.
- **Componible:** un efecto se construye combinando componentes reutilizables.
- **Separación clara:** renderizado, animación, simulación y red son responsabilidades distintas.
- **Orientado a escenas:** los efectos podrán tener fases, curvas temporales, transiciones y parámetros.
- **Multijugador consciente:** el servidor controla la lógica que afecta al mundo; el cliente dibuja los efectos.
- **Rendimiento medible:** evitar trabajo innecesario por fotograma y permitir ajustar la calidad.
- **Extensible:** preparar formatos de definición para efectos sencillos y APIs Java para comportamientos especiales.

## Empezar

Se necesita un JDK 17 y Gradle 8.1.1 instalado.

```bash
gradle build
```

El archivo JAR se genera en `build/libs/`. Para ejecutar Minecraft desde el entorno de desarrollo, Gradle Forge proporciona las tareas `runClient` y `runServer`.

## Estructura prevista

- `core/effect`: contratos y registro de efectos.
- `core/runtime`: ciclo de vida de los efectos activos.
- `client/render`: renderizado personalizado y coordinación de pases.
- `client/shader`: carga de shaders y materiales.
- `client/particle`: partículas, estelas y emisores.
- `client/postprocess`: composición y efectos de pantalla.
- `timeline`: fases, curvas y secuencias temporales.
- `network`: definiciones de sincronización.
- `data`: definiciones de efectos y parámetros externos.

La estructura evolucionará según lo que demuestren los primeros prototipos. No se añadirán sistemas complejos sin una necesidad real.

## Documentación

- [Arquitectura](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Diseño de efectos](docs/CREAR_EFECTOS.md)

## Estado actual

La primera etapa establece el proyecto Forge, el contrato básico de efecto, un administrador de ciclo de vida, documentación y compilación de CI. La integración real con el renderizado, los shaders GLSL y el posprocesamiento se añadirá en pasos pequeños y verificables.

## Licencia

Todavía no se ha elegido una licencia. Hasta que se añada una, no se debe asumir que el código puede redistribuirse o reutilizarse libremente.
