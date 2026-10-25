# Arquitectura del motor VFX

## Objetivo

Motor de efectos visuales en tiempo real para Forge 1.20.1. El motor admite efectos ambientales, geometría colocada en coordenadas del mundo, efectos ligados a entidades y pases que deforman la imagen completa de la cámara.

## Definiciones e instancias

EffectRegistry mantiene las fábricas, categorías y metadatos. Una definición describe el tipo de VFX. WorldVfxInstance representa una aparición concreta con su identificador, dimensión, posición, rotación, escala, duración y parámetros numéricos. Una definición puede tener muchas apariciones simultáneas.

El agujero negro blackhole usa el diámetro (en bloques) y las coordenadas recibidas por el comando. No se mueve automáticamente con el jugador. Al entrar dentro del radio de su horizonte, el renderer deja de dibujar la esfera opaca para no bloquear la vista como una pared sólida.

## Renderizado espacial

ClientWorldVfxRenderer dibuja instancias concretas con VfxRenderContext, que contiene el nivel, la cámara, las coordenadas relativas, el pose stack, la etapa y el tiempo parcial. ClientWorldVfxRendererRegistry enlaza los tipos colocados con su renderer.

## Posprocesamiento

BlackHoleLensingPass proyecta agujeros negros colocados y el agujero negro celeste a coordenadas de pantalla. El shader GLSL deforma las UV de la textura renderizada y procesa el cielo y los bloques juntos. La escena se renderiza en un target auxiliar y luego se copia al target principal para no leer y escribir sobre la misma textura a la vez.

Los dos VFX tienen funciones distintas:

- blackhole: anclado a coordenadas del mundo, con tamaño en bloques y posición configurables.
- blackhole_sky: efecto ambiental ubicado en una dirección celeste; su posición se elige al activarlo y permanece en esa dirección visual.

La lente es una distorsión de imagen local al cliente: no cambia bloques reales, colisiones ni fuerzas físicas.

## Otros efectos

music_horizon_bars y rainbow_aurora usan ondas con frecuencias enteras en relación con la vuelta completa del círculo para que los extremos sean continuos. rainbow_sky dibuja una cúpula translúcida cuyo matiz cambia gradualmente.

## Comandos y autoridad

Los comandos /vfx pertenecen al cliente y controlan efectos visuales locales. En cambio, /vfxmove empujon se registra en el servidor, requiere nivel de permiso 2 y aplica un cambio real de velocidad a cada jugador seleccionado. El impulso horizontal va en sentido contrario a la mirada de cada objetivo y se combina con un impulso vertical moderado.

La sincronización de instancias visuales con otros clientes aún está pendiente. Las acciones físicas que alteran jugadores deben ejecutarse desde el servidor.

## Estado de implementación

Implementado: registro y categorías, runtime local, comandos VFX, efectos de cielo, instancia espacial de agujero negro, lente gravitacional GLSL, agujero negro celeste y comando de empujón de jugadores.

Pendiente: pruebas visuales dentro del cliente, ajuste de la deformación y rendimiento, materiales generales, partículas y estelas avanzadas, sincronización multijugador de VFX.
