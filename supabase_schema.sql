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
    monthly_income NUMERIC NOT NULL DEFAULT 0,
    monthly_expense NUMERIC NOT NULL DEFAULT 0,
    categories JSONB NOT NULL DEFAULT '[]'::jsonb,
    transactions JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_live BOOLEAN NOT NULL DEFAULT true,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

-- 3. Habilita RLS (Row Level Security) e libera leitura e escrita pública/anônima
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

-- 4. Habilita Realtime do Supabase nas tabelas
ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_market_lists;
ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_finance_dashboards;
