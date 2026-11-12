using Bitacora.Domain;
namespace Bitacora.Application;
public sealed class RegistroService(IRegistroRepository repository)
{
    private static RegistroDetail Map(Registro r) => new(r.Id, r.Titulo, r.Descripcion, r.FotoBase64, r.FechaCreacion);
    public Task<IReadOnlyList<RegistroListItem>> Listar(CancellationToken ct) => repository.Listar(ct);
    public async Task<RegistroDetail?> Consultar(int id, CancellationToken ct) =>
        await repository.Buscar(id, ct) is { } r ? Map(r) : null;
    public async Task<RegistroDetail> Crear(RegistroInput input, CancellationToken ct)
    {
        var r = new Registro(input.Titulo, input.Descripcion, input.FotoBase64);
        await repository.Agregar(r, ct);
        return Map(r);
    }
    public async Task<RegistroDetail?> Actualizar(int id, RegistroInput input, CancellationToken ct)
    {
        var r = await repository.Buscar(id, ct);
        if (r is null) return null;
        r.Actualizar(input.Titulo, input.Descripcion, input.FotoBase64);
        await repository.Guardar(ct);
        return Map(r);
    }
    public async Task<bool> Eliminar(int id, CancellationToken ct)
    {
        var r = await repository.Buscar(id, ct);
        if (r is null) return false;
        await repository.Eliminar(r, ct);
        return true;
    }
}
