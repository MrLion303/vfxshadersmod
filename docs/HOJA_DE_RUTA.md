# Hoja de ruta

La prioridad es construir un motor gráfico fiable y ampliarlo con componentes reutilizables. Esta lista distingue las bases que ya están en el repositorio de las funciones que todavía deben desarrollarse.

## Fase 1 — Proyecto y control de efectos
- [x] Proyecto Forge 1.20.1 con Java 17.
- [x] Compilación automática en GitHub Actions.
- [x] Contrato inicial, registro de fábricas y runtime.
- [x] Comandos locales `/vfx list`, `enable`, `disable`, `toggle`, `status` y `disable all`.
- [x] Primer VFX de prueba con geometría de líneas.
- [ ] Verificar el resultado de la compilación de CI.
- [ ] Probar en Minecraft cliente y servidor dedicado.

## Fase 2 — Renderizado propio
- [ ] Establecer un contrato de renderizado por etapa del mundo.
- [ ] Dibujar mallas personalizadas y objetos orientados hacia cámara.
- [ ] Crear materiales básicos con color, opacidad y emisión.
- [ ] Gestionar recarga y liberación de recursos.

## Fase 3 — Herramientas de VFX
- [ ] Cargador y registro de shaders GLSL.
- [ ] Componentes de partículas y emisores.
- [ ] Estelas con control de longitud, anchura y transparencia.
- [ ] Curvas de animación, fases y transiciones.
- [ ] Parámetros ajustables por efecto.
- [ ] Diagnóstico legible si un shader no compila o faltan recursos.

## Fase 4 — Posprocesamiento
- [ ] Prototipo aislado de un pase de pantalla.
- [ ] Infraestructura para distorsión, gradación de color y bloom.
- [ ] Ajustes de calidad y comportamiento seguro si un pase no está disponible.

## Fase 5 — Primeras demostraciones
- [ ] Meteorito con núcleo incandescente, estela y humo.
- [ ] Portal con anillos, distorsión y salida de una entidad visual.
- [ ] Fenómeno de agujero negro con disco de acreción y distorsión.
- [ ] Supernova con fases temporales y variación del cielo.
- [ ] Secuencias de cielo: auroras, nubes y cuerpos celestes.

## Fase 6 — Autoría y multijugador
- [ ] Definiciones externas para efectos compuestos sencillos.
- [ ] API de componentes que otros efectos puedan reutilizar.
- [ ] Mensajes de red para eventos visuales sincronizados.
- [ ] Mecánicas autoritativas de servidor para fuerzas, daño o cambios de mundo.
- [ ] Herramientas de depuración, perfilado y documentación para creadores.
- [ ] Editor visual de composición de efectos.

## Criterios de avance

No considerar una función terminada hasta que compile, se haya probado en el juego y tenga claro su comportamiento en servidor dedicado, recursos recargados y equipos de rendimiento limitado cuando corresponda.
