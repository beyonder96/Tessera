-- ==============================================================================
-- TESSERA SUPABASE SCHEMA: Shared Tasks & Notices Hub
-- ==============================================================================

CREATE TABLE IF NOT EXISTS public.shared_tasks_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Tarefas e Lembretes',
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Enable Realtime Publication (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_tasks_hub'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_tasks_hub;
    END IF;
END $$;

-- Row Level Security (RLS)
ALTER TABLE public.shared_tasks_hub ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Anon All shared_tasks_hub" ON public.shared_tasks_hub;
CREATE POLICY "Public Anon All shared_tasks_hub" 
ON public.shared_tasks_hub 
FOR ALL 
TO anon, authenticated 
USING (true) 
WITH CHECK (true);
