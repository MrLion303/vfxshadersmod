# VFX Shaders Mod

Motor independiente de efectos visuales en tiempo real para Minecraft Java Edition, orientado a **Forge 1.20.1**.

El objetivo no es añadir solamente una lista de efectos prefabricados. Queremos una base extensible para combinar shaders GLSL, geometría personalizada, partículas, animaciones, posprocesamiento y comportamiento del mundo. La meta es crear escenas cinematográficas —meteoritos con humo, portales, anomalías gravitacionales, supernovas y cielos dinámicos— sin rehacer el sistema para cada efecto nuevo.

> **Estado:** estructura del motor, comandos locales, primer efecto de prueba y shader GLSL básico incluidos. Los efectos cinematográficos complejos, el posprocesamiento y el editor visual todavía están pendientes.

## Comandos dentro del juego

- `/vfx list`: lista todos los efectos registrados y señala cuáles están activos.
- `/vfx enable <efecto>`: activa uno.
- `/vfx disable <efecto>`: desactiva uno.
- `/vfx toggle <efecto>`: alterna su estado.
- `/vfx status <efecto>`: consulta su estado.
- `/vfx disable all`: desactiva todos.
- `/vfx help`: muestra la ayuda.

Los argumentos tienen autocompletado con Tab. Puedes escribir `demo_orbit` o el identificador completo `vfxshadersmod:demo_orbit`.

Los comandos son del cliente y solo afectan los efectos visuales de tu instancia; funcionan también al jugar en un servidor sin requerir permisos de operador. Las mecánicas que alteren jugadores, criaturas o bloques deberán ejecutarse y validarse en el servidor.

### VFX incluido como prueba

- **Órbitas de demostración** (`demo_orbit`): tres anillos de color alrededor del jugador local, usados como prueba del motor.
- **Barras musicales del horizonte** (`music_horizon_bars`): barras de ecualizador animadas en un anillo alrededor del horizonte.
- **Aurora boreal arcoíris** (`rainbow_aurora`): cortinas de luz ondulantes con colores que recorren el arcoíris.

Ambos efectos se pueden activar juntos. Las barras usan una animación procedural; todavía no reciben el audio real del juego o del sistema.

Consulta la [guía de comandos](docs/COMANDOS.md).

## Arquitectura

El catálogo central guarda el identificador, nombre visible, descripción y fábrica de cada efecto. `/vfx list`, el estado activo y el autocompletado se generan desde ese registro: al crear un efecto nuevo, se añade su fábrica a `ClientVfxCatalog` y aparece en la lista.

- **Núcleo:** ciclo de vida, identificadores y registro.
- **Renderizado:** componentes del lado cliente que dibujan en las etapas del mundo.
- **Shaders y materiales:** la base GLSL se carga desde `VfxShaderLibrary`; `VfxRenderTypes` contiene tipos de renderizado reutilizables.
- **Partículas y estelas:** sistemas previstos para efectos compuestos.
- **Posprocesamiento:** infraestructura futura para distorsión, bloom y composición de pantalla.
- **Simulación y red:** separación entre visuales locales y comportamiento autoritativo del mundo.

## Plataforma y compilación

- Minecraft Java Edition 1.20.1
- Minecraft Forge 47.4.18
- Java 17
- Gradle 8.1.1

Se necesita un JDK 17 y Gradle 8.1.1 instalado:

```bash
gradle build
```

El JAR se genera en `build/libs/`. GitHub Actions compila en cada push a `main` y guarda el JAR como artefacto al terminar correctamente.

## Documentación

- [Arquitectura del motor](docs/ARQUITECTURA.md)
- [Hoja de ruta](docs/HOJA_DE_RUTA.md)
- [Cómo crear un efecto nuevo](docs/CREAR_EFECTOS.md)
- [Comandos VFX](docs/COMANDOS.md)

## Qué falta

Aunque ya existe una base GLSL y un tipo de renderizado compartido, aún debe validarse la carga en una sesión real del cliente de Minecraft. También faltan el cargador de materiales reutilizables, partículas avanzadas, estelas, posprocesamiento, definiciones externas de efectos, eventos sincronizados para multijugador y el editor visual.

## Independencia y licencia

Este proyecto es independiente y no modifica ni depende del repositorio `shadermod`.

Todavía no se ha elegido una licencia. Hasta que se añada una, no se debe asumir que el código puede redistribuirse o reutilizarse libremente.
