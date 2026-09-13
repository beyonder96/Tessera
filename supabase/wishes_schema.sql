-- ==============================================================================
-- TESSERA SUPABASE SCHEMA: Shared Wishes & Purchase Goals Hub
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.shared_wishes_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Lista de Desejos',
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Realtime Publication (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_wishes_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_wishes_hub;
    END IF;
END $$;

-- Row Level Security (RLS)
ALTER TABLE public.shared_wishes_hub ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Anon All shared_wishes_hub" ON public.shared_wishes_hub;
CREATE POLICY "Public Anon All shared_wishes_hub" 
ON public.shared_wishes_hub 
FOR ALL 
TO anon, authenticated 
USING (true) 
WITH CHECK (true);
