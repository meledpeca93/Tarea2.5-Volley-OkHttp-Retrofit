using Bitacora.Application;
using Bitacora.Domain;
using Microsoft.EntityFrameworkCore;
namespace Bitacora.Infrastructure;
public sealed class RegistroRepository(BitacoraDbContext db) : IRegistroRepository
{
    public async Task<IReadOnlyList<RegistroListItem>> Listar(CancellationToken ct) =>
        await db.Registros.AsNoTracking().OrderByDescending(r => r.Id)
            .Select(r => new RegistroListItem(r.Id, r.Titulo, r.Descripcion, r.FechaCreacion)).ToListAsync(ct);
    public Task<Registro?> Buscar(int id, CancellationToken ct) => db.Registros.SingleOrDefaultAsync(r => r.Id == id, ct);
    public async Task Agregar(Registro registro, CancellationToken ct)
    { db.Registros.Add(registro); await db.SaveChangesAsync(ct); }
    public async Task Guardar(CancellationToken ct) { await db.SaveChangesAsync(ct); }
    public async Task Eliminar(Registro registro, CancellationToken ct)
    { db.Registros.Remove(registro); await db.SaveChangesAsync(ct); }
}
