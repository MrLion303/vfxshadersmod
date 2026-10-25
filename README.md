# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a **Forge 1.20.1**.

El objetivo no es añadir solamente una lista de efectos prefabricados. Queremos una base extensible para combinar shaders, geometría personalizada, partículas, animaciones, posprocesamiento y comportamiento del mundo. La meta es crear escenas cinematográficas —meteoritos con humo, portales, anomalías gravitacionales, supernovas y cielos dinámicos— sin rehacer el sistema para cada nuevo efecto.

> **Estado:** estructura inicial, runtime cliente y comandos de control implementados. Los efectos cinematográficos complejos y el editor visual todavía están pendientes.

## Plataforma

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

## Comandos dentro del juego

- `/vfx list`: lista los efectos registrados y marca cuáles están activos.
- `/vfx enable <efecto>`: activa uno.
- `/vfx disable <efecto>`: desactiva uno.
- `/vfx toggle <efecto>`: alterna su estado.
- `/vfx status <efecto>`: consulta su estado.
- `/vfx disable all`: desactiva todos.
- `/vfx help`: muestra la ayuda.

Los comandos se ejecutan en el cliente, también al jugar en servidores. Por eso los efectos visuales no requieren permisos de operador. Las mecánicas que alteren el mundo deberán sincronizarse y validarse en el servidor.

El primer efecto de prueba es `demo_orbit`, tres anillos de color alrededor del jugador local. Está para probar los comandos y el renderizado; no es uno de los efectos cinematográficos finales.

Consulta [todos los comandos](docs/COMANDOS.md).

## Principios del proyecto

- **Independiente:** no modifica ni depende del repositorio `shadermod`.
- **Componible:** un efecto se construye combinando componentes reutilizables.
- **Separación clara:** renderizado, animación, simulación y red son responsabilidades distintas.
- **Orientado a escenas:** los efectos pueden tener fases, curvas temporales, transiciones y parámetros.
- **Multijugador consciente:** el servidor controla la lógica que afecta al mundo; el cliente dibuja los efectos.
- **Rendimiento medible:** evitar trabajo innecesario por fotograma y permitir ajustar la calidad.
- **Extensible:** la lista de comandos se genera desde el registro central, sin mantener una segunda lista.

## Compilar

Se necesita un JDK 17 y Gradle 8.1.1.

```bash
gradle build
```

El JAR se genera en `build/libs/`. Gradle Forge proporciona las tareas `runClient` y `runServer` para probar el entorno de desarrollo. GitHub Actions ejecuta la compilación en cada push a `main` y almacena el JAR como artefacto cuando finaliza correctamente.

## Arquitectura y documentos

- [Arquitectura del motor](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Cómo diseñar efectos nuevos](docs/CREAR_EFECTOS.md)
- [Comandos VFX](docs/COMANDOS.md)

## Estado actual

Implementado: configuración Forge, contrato común para efectos, registro de fábricas, runtime cliente, activación/desactivación mediante comandos, renderizado de prueba y compilación automática configurada.

Pendiente: cargador y gestión de shaders GLSL, materiales reutilizables, partículas avanzadas, estelas, cadena de posprocesamiento, definiciones externas de efectos, multijugador para eventos visuales y mecánicas y editor visual. La existencia de estas ideas en la documentación no significa que ya estén implementadas.

## Licencia

Todavía no se ha elegido una licencia. Hasta que se añada una, no se debe asumir que el código puede redistribuirse o reutilizarse libremente.
