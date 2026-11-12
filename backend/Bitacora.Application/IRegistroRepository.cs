using Bitacora.Domain;
namespace Bitacora.Application;
public interface IRegistroRepository
{
    Task<IReadOnlyList<RegistroListItem>> Listar(CancellationToken ct);
    Task<Registro?> Buscar(int id, CancellationToken ct);
    Task Agregar(Registro registro, CancellationToken ct);
    Task Guardar(CancellationToken ct);
    Task Eliminar(Registro registro, CancellationToken ct);
}
