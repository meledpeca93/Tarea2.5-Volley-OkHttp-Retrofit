using Bitacora.Domain;
using Microsoft.EntityFrameworkCore;
namespace Bitacora.Infrastructure;
public sealed class BitacoraDbContext(DbContextOptions<BitacoraDbContext> options) : DbContext(options)
{
    public DbSet<Registro> Registros => Set<Registro>();
    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        var entity = modelBuilder.Entity<Registro>();
        entity.HasKey(r => r.Id);
        entity.Property(r => r.Titulo).HasMaxLength(100).IsRequired();
        entity.Property(r => r.Descripcion).HasMaxLength(500).IsRequired();
    }
}
