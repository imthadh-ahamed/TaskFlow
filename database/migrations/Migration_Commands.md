# 1. Confirm the container is healthy
docker compose ps

# 2. Copy the migration file into the container
docker cp database/migrations/V1__initial_schema.sql taskflow-postgres:/tmp/V1__initial_schema.sql

# 3. Apply the migration
docker exec taskflow-postgres psql -U postgres -d TaskFlow -f /tmp/V1__initial_schema.sql

# 4. Verify the four tables exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "\dt public.*"

# 5. Verify all 14 constraints exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "
select conrelid::regclass as table_name, conname, contype
from pg_constraint
where conrelid::regclass::text in ('users','projects','project_members','tasks')
order by table_name, contype, conname;"

# 6. Verify the three indexes exist
docker exec taskflow-postgres psql -U postgres -d TaskFlow -c "
select indexname, tablename
from pg_indexes
where tablename in ('users','projects','project_members','tasks')
  and schemaname = 'public'
order by tablename, indexname;"

# 7. Clean up the temp file from the container
docker exec taskflow-postgres rm /tmp/V1__initial_schema.sql

