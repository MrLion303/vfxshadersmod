# Comandos de VFX

Los comandos se ejecutan localmente en el cliente, por lo que funcionan tanto en un mundo individual como al jugar en un servidor sin requerir permisos de operador. Solo afectan a los efectos visuales locales; las mecánicas que cambien el mundo necesitarán sincronización y validación por separado.

## Comandos disponibles

| Comando | Función |
|---|---|
| `/vfx` o `/vfx help` | Muestra la ayuda |
| `/vfx list` | Enumera todos los VFX registrados y marca si están activos |
| `/vfx enable <efecto>` | Activa un efecto |
| `/vfx disable <efecto>` | Desactiva un efecto |
| `/vfx toggle <efecto>` | Alterna entre activo e inactivo |
| `/vfx status <efecto>` | Consulta el estado de un efecto |
| `/vfx disable all` | Desactiva todos los efectos |

Los argumentos sugieren los identificadores registrados al pulsar Tab. También se acepta el nombre corto de los efectos del mod.

## Efecto de prueba

- `demo_orbit`: tres anillos de color alrededor del jugador local. Se incluye para comprobar el registro, el ciclo de vida, el renderizado y los comandos; no pretende ser uno de los VFX cinematográficos finales.

Ejemplo:

```text
/vfx list
/vfx enable demo_orbit
/vfx status demo_orbit
/vfx toggle demo_orbit
/vfx disable all
```

## Añadir un efecto nuevo a la lista

1. Crear una implementación de `VfxEffect` para su estado y ciclo de vida.
2. Implementar `ClientVfxRenderer` en el efecto o en su renderer asociado si necesita dibujar en el cliente.
3. Registrar una fábrica en `ClientVfxCatalog.registerDefaults()` con un identificador único.
4. Añadir la descripción y los comandos de prueba a esta documentación.

El listado y las sugerencias de los comandos se generan desde el registro. No hace falta mantener una segunda lista manual de identificadores en el código de comandos.
