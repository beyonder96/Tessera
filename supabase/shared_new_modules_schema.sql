-- ==============================================================================
-- TESSERA SUPABASE SCHEMA: Realtime Hubs (Saúde, Rotinas, Pets e Transporte)
-- ==============================================================================

-- 1. Shared Health Hub (Hidratação, Peso, Passos, Sono, Remédios)
CREATE TABLE IF NOT EXISTS public.shared_health_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Saúde & Bem-Estar',
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Shared Routines & Habits Hub (Hábitos Diários, Streaks, Rotinas Matinal/Noturna)
CREATE TABLE IF NOT EXISTS public.shared_routines_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Rotinas & Hábitos',
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Shared Pets Hub (Pets, Vacinas, Consultas, Medicamentos, Cuidados)
CREATE TABLE IF NOT EXISTS public.shared_pets_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Central Petz',
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 4. Shared Transport Hub (Linhas Favoritas do Metrô, Trem CPTM e Ônibus SPTrans)
CREATE TABLE IF NOT EXISTS public.shared_transport_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Transporte & Mobilidade',
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- ==============================================================================
-- 5. Habilitar Realtime Publications (idempotente)
-- ==============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_health_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_health_hub;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_routines_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_routines_hub;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_pets_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_pets_hub;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_transport_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_transport_hub;
    END IF;
END $$;

-- ==============================================================================
-- 6. Row Level Security (RLS) Blindado
-- ==============================================================================
ALTER TABLE public.shared_health_hub ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_routines_hub ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_pets_hub ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_transport_hub ENABLE ROW LEVEL SECURITY;

-- Remove políticas anteriores caso existam
DROP POLICY IF EXISTS "Public Anon All shared_health_hub" ON public.shared_health_hub;
DROP POLICY IF EXISTS "Public Anon All shared_routines_hub" ON public.shared_routines_hub;
DROP POLICY IF EXISTS "Public Anon All shared_pets_hub" ON public.shared_pets_hub;
DROP POLICY IF EXISTS "Public Anon All shared_transport_hub" ON public.shared_transport_hub;

-- Políticas de acesso aberto controlado para o app e service_role
CREATE POLICY "Public Anon All shared_health_hub" 
ON public.shared_health_hub FOR ALL TO anon, authenticated, service_role 
USING (true) WITH CHECK (true);

CREATE POLICY "Public Anon All shared_routines_hub" 
ON public.shared_routines_hub FOR ALL TO anon, authenticated, service_role 
USING (true) WITH CHECK (true);

CREATE POLICY "Public Anon All shared_pets_hub" 
ON public.shared_pets_hub FOR ALL TO anon, authenticated, service_role 
USING (true) WITH CHECK (true);

CREATE POLICY "Public Anon All shared_transport_hub" 
ON public.shared_transport_hub FOR ALL TO anon, authenticated, service_role 
USING (true) WITH CHECK (true);
