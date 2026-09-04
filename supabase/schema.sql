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

-- 4. Row Level Security (RLS)
ALTER TABLE public.shared_market_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_finance_dashboards ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public Anon All shared_market_lists" ON public.shared_market_lists;
CREATE POLICY "Public Anon All shared_market_lists" 
ON public.shared_market_lists 
FOR ALL 
TO anon, authenticated 
USING (true) 
WITH CHECK (true);

DROP POLICY IF EXISTS "Public Anon All shared_finance_dashboards" ON public.shared_finance_dashboards;
CREATE POLICY "Public Anon All shared_finance_dashboards" 
ON public.shared_finance_dashboards 
FOR ALL 
TO anon, authenticated 
USING (true) 
WITH CHECK (true);
