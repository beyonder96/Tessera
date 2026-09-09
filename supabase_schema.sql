-- ==============================================================================
-- TESSERA: Schema SQL para Supabase (Listas de Mercado & Resumo Financeiro)
-- ==============================================================================

-- 1. Tabela para Compartilhamento em Tempo Real de Mercado
CREATE TABLE IF NOT EXISTS public.shared_market_lists (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Lista de Mercado',
    items JSONB NOT NULL DEFAULT '[]'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 2. Tabela para Compartilhamento em Tempo Real de Finanças
CREATE TABLE IF NOT EXISTS public.shared_finance_dashboards (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL DEFAULT 'Resumo Financeiro',
    month_label TEXT NOT NULL DEFAULT '',
    total_balance NUMERIC NOT NULL DEFAULT 0,
    spendable_balance NUMERIC NOT NULL DEFAULT 0,
    salary_value NUMERIC NOT NULL DEFAULT 0,
    committed_value NUMERIC NOT NULL DEFAULT 0,
    committed_percentage NUMERIC NOT NULL DEFAULT 0,
    categories JSONB NOT NULL DEFAULT '[]'::jsonb,
    transactions JSONB NOT NULL DEFAULT '[]'::jsonb,
    suggestions JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_live BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Migrações incrementais caso a tabela já tenha sido criada:
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS spendable_balance NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS salary_value NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS committed_value NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS committed_percentage NUMERIC NOT NULL DEFAULT 0;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS suggestions JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS debts JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS installments JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS recurrents JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS accounts JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE public.shared_finance_dashboards ADD COLUMN IF NOT EXISTS cards JSONB NOT NULL DEFAULT '[]'::jsonb;

-- 3. Habilita RLS (Row Level Security) Blindado
ALTER TABLE public.shared_market_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_finance_dashboards ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Anon All shared_market_lists" ON public.shared_market_lists;
DROP POLICY IF EXISTS "Public Anon All shared_finance_dashboards" ON public.shared_finance_dashboards;

-- shared_market_lists
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

-- shared_finance_dashboards
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

-- 4. Habilita Realtime do Supabase nas tabelas de forma segura (idempotente)
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

-- 5. Função RPC com SECURITY DEFINER para consulta segura por ID único
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
        RAISE EXCEPTION 'Tabela não autorizada: %', p_table;
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

