# Hoja de ruta

La prioridad es crear una base gráfica fiable y ampliar las capacidades solo cuando estén probadas. Esta lista representa el plan inicial, no funciones que ya estén terminadas.

## Fase 1 — Proyecto y runtime
- [x] Proyecto Forge 1.20.1 con Java 17.
- [x] Compilación automática en GitHub Actions.
- [x] Contrato inicial, registro de fábricas y administrador de ciclo de vida.
- [ ] Primera ejecución verificada de la compilación en CI.
- [ ] Primer evento de cliente que actualice un efecto de prueba.

## Fase 2 — Renderizado propio
- [ ] Dibujar una malla simple dentro del mundo usando las etapas de renderizado correctas.
- [ ] Crear materiales con color, opacidad y emisión.
- [ ] Definir el contrato de renderizado de cliente sin contaminar el código común.
- [ ] Gestionar recarga y liberación de recursos.

## Fase 3 — Herramientas de VFX
- [ ] Componentes de partículas y emisores.
- [ ] Estelas y geometría orientada a cámara.
- [ ] Curvas de animación, fases y transiciones.
- [ ] Carga de shaders GLSL y validación de errores legible.
- [ ] Parámetros ajustables por efecto.

## Fase 4 — Posprocesamiento
- [ ] Prototipo aislado de un pase de pantalla.
- [ ] Infraestructura para distorsión, gradación de color y bloom.
- [ ] Ajustes de calidad y comportamiento seguro si un pase no está disponible.

## Fase 5 — Primeras demostraciones
- [ ] Meteorito con núcleo incandescente, estela y humo.
- [ ] Portal con anillos, distorsión y salida de una entidad visual.
- [ ] Fenómeno de agujero negro con disco de acreción y distorsión.
- [ ] Supernova con fases temporales y variación del cielo.

## Fase 6 — Autoría y multijugador
- [ ] Definición externa para efectos compuestos sencillos.
- [ ] API de componentes que otros efectos puedan reutilizar.
- [ ] Mensajes de red para eventos visuales sincronizados.
- [ ] Herramientas de depuración, perfiles de rendimiento y documentación para creadores.

## Criterios de avance

No considerar una función terminada hasta que compile, se haya probado en el juego y tenga claro su comportamiento en servidor dedicado, recursos recargados y equipos de rendimiento limitado cuando corresponda.
