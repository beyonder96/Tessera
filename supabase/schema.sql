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
    month_label TEXT NOT NULL DEFAULT 'Agosto 2026',
    total_balance NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monthly_income NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    monthly_expense NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    categories JSONB NOT NULL DEFAULT '[]'::jsonb,
    transactions JSONB NOT NULL DEFAULT '[]'::jsonb,
    is_live BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- 3. Enable Realtime Publications
ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_market_lists;
ALTER PUBLICATION supabase_realtime ADD TABLE public.shared_finance_dashboards;

-- 4. Row Level Security (RLS)
ALTER TABLE public.shared_market_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.shared_finance_dashboards ENABLE ROW LEVEL SECURITY;

-- Allow public read and write by ID for shared rooms
CREATE POLICY "Public Read Access for Market Lists" ON public.shared_market_lists
    FOR SELECT USING (true);

CREATE POLICY "Public Insert/Update Access for Market Lists" ON public.shared_market_lists
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Public Read Access for Finance Dashboards" ON public.shared_finance_dashboards
    FOR SELECT USING (true);

CREATE POLICY "Public Insert/Update Access for Finance Dashboards" ON public.shared_finance_dashboards
    FOR ALL USING (true) WITH CHECK (true);
