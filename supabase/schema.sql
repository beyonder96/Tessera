-- ==============================================================================
-- TESSERA SUPABASE SCHEMA: Realtime Web Sync (Market & Finance)
-- ==============================================================================

-- 1. Shared Market Lists Table
CREATE TABLE IF NOT EXISTS public.shared_market_lists (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Lista de Compras',
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 2. Shared Finance Dashboards Table
CREATE TABLE IF NOT EXISTS public.shared_finance_dashboards (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Resumo Financeiro',
    month_label TEXT NOT NULL DEFAULT 'Mês Atual',
    total_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    spendable_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    salary_value NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    committed_value NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    committed_percentage NUMERIC(5,2) NOT NULL DEFAULT 0.00,
    categories JSONB NOT NULL DEFAULT '[]'::jsonb,
    transactions JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggestions JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_live BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Migrações incrementais caso a tabela já tenha sido criada:
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS spendable_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS salary_value NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS committed_value NUMERIC(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS committed_percentage NUMERIC(5,2) NOT NULL DEFAULT 0.00;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS suggestions JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS debts JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS installments JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS recurrents JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS accounts JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS cards JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 3. Enable Realtime Publications de forma segura (idempotente)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_market_lists'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_market_lists;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_publication_tables 
        WHERE pubname = 'supabase_realtime' 
          AND schemaname = 'public' 
          AND tablename = 'shared_finance_dashboards'
    ) THEN
        ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_finance_dashboards;
    END IF;
END $$;

-- 4. Row Level Security (RLS) Blindado
ALTER TABLE public.shared_market_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_finance_dashboards ENABLE ROW LEVEL SECURITY;

-- Remove políticas públicas irrestritas antigas
DROP POLICY IF EXISTS "Public Anon All shared_market_lists" ON public.shared_market_lists;
DROP POLICY IF EXISTS "Public Anon All shared_finance_dashboards" ON public.shared_finance_dashboards;

-- shared_market_lists: Permite INSERT e UPDATE apenas para registros válidos (sem permissão de DELETE para anon)
CREATE POLICY "Anon Insert shared_market_lists" 
ON public.shared_market_lists 
FOR INSERT 
TO anon, authenticated 
WITH CHECK (id IS NOT NULL AND length(trim(id)) > 0);

CREATE POLICY "Anon Update shared_market_lists" 
ON public.shared_market_lists 
FOR UPDATE 
TO anon, authenticated 
USING (id IS NOT NULL) 
WITH CHECK (id IS NOT NULL);

CREATE POLICY "Anon Select shared_market_lists" 
ON public.shared_market_lists 
FOR SELECT 
TO anon, authenticated 
USING (id IS NOT NULL);

-- shared_finance_dashboards: Permite INSERT e UPDATE específicos (sem permissão de DELETE para anon)
CREATE POLICY "Anon Insert shared_finance_dashboards" 
ON public.shared_finance_dashboards 
FOR INSERT 
TO anon, authenticated 
WITH CHECK (id IS NOT NULL AND length(trim(id)) > 0);

CREATE POLICY "Anon Update shared_finance_dashboards" 
ON public.shared_finance_dashboards 
FOR UPDATE 
TO anon, authenticated 
USING (id IS NOT NULL) 
WITH CHECK (id IS NOT NULL);

CREATE POLICY "Anon Select shared_finance_dashboards" 
ON public.shared_finance_dashboards 
FOR SELECT 
TO anon, authenticated 
USING (id IS NOT NULL);

-- 5. Shared Tasks & Notices Hub Table
CREATE TABLE IF NOT EXISTS public.shared_tasks_hub (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Tarefas e Lembretes',
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

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

ALTER TABLE public.shared_tasks_hub ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Anon All shared_tasks_hub" ON public.shared_tasks_hub;

CREATE POLICY "Anon Insert shared_tasks_hub" 
ON public.shared_tasks_hub 
FOR INSERT 
TO anon, authenticated 
WITH CHECK (id IS NOT NULL AND length(trim(id)) > 0);

CREATE POLICY "Anon Update shared_tasks_hub" 
ON public.shared_tasks_hub 
FOR UPDATE 
TO anon, authenticated 
USING (id IS NOT NULL) 
WITH CHECK (id IS NOT NULL);

CREATE POLICY "Anon Select shared_tasks_hub" 
ON public.shared_tasks_hub 
FOR SELECT 
TO anon, authenticated 
USING (id IS NOT NULL);

-- 6. Funções RPC Seguras com SECURITY DEFINER (Prevenção de scraping/varredura em lote)
-- Exige obrigatoriamente um identificador específico para retornar o documento correspondente.
CREATE OR REPLACE FUNCTION public.get_shared_document(p_table text, p_id text)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_result jsonb;
BEGIN
    IF p_table NOT IN ('shared_finance_dashboards', 'shared_market_lists', 'shared_tasks_hub') THEN
        RAISE EXCEPTION 'Tabela não autorizada para consulta compartilhada: %', p_table;
    END IF;
    
    IF p_id IS NULL OR length(trim(p_id)) = 0 THEN
        RETURN NULL;
    END IF;

    EXECUTE format('SELECT to_jsonb(t) FROM public.%I t WHERE t.id = $1 LIMIT 1', p_table)
    INTO v_result
    USING p_id;
    
    RETURN v_result;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_shared_document(text, text) TO anon, authenticated;


