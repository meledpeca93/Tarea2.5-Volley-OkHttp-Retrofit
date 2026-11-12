using Bitacora.Application;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
namespace Bitacora.Api.Controllers;
[Authorize]
[ApiController]
[Route("api/registros")]
[Produces("application/json")]
public sealed class RegistrosController(RegistroService service) : ControllerBase
{
    [HttpGet]
    [ProducesResponseType<IReadOnlyList<RegistroListItem>>(200)]
    public async Task<IActionResult> Listar(CancellationToken ct) => Ok(await service.Listar(ct));

    [HttpGet("{id:int}")]
    [ProducesResponseType<RegistroDetail>(200)]
    [ProducesResponseType<ProblemDetails>(404)]
    public async Task<IActionResult> Consultar(int id, CancellationToken ct) =>
        await service.Consultar(id, ct) is { } r ? Ok(r) : NotFound();

    [HttpPost]
    [ProducesResponseType<RegistroDetail>(201)]
    [ProducesResponseType<ValidationProblemDetails>(400)]
    public async Task<IActionResult> Crear(RegistroInput input, CancellationToken ct)
    {
        var r = await service.Crear(input, ct);
        return CreatedAtAction(nameof(Consultar), new { id = r.Id }, r);
    }

    [HttpPut("{id:int}")]
    [ProducesResponseType<RegistroDetail>(200)]
    [ProducesResponseType<ValidationProblemDetails>(400)]
    [ProducesResponseType<ProblemDetails>(404)]
    public async Task<IActionResult> Actualizar(int id, RegistroInput input, CancellationToken ct) =>
        await service.Actualizar(id, input, ct) is { } r ? Ok(r) : NotFound();

    [HttpDelete("{id:int}")]
    [ProducesResponseType(204)]
    [ProducesResponseType<ProblemDetails>(404)]
    public async Task<IActionResult> Eliminar(int id, CancellationToken ct) =>
        await service.Eliminar(id, ct) ? NoContent() : NotFound();
}
