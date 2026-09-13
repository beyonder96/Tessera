-- ==============================================================================
-- TESSERA: INTEGRAÇÃO TELEGRAM BOT VIA SUPABASE EDGE FUNCTIONS
-- ==============================================================================

-- 1. Permissões de Leitura e Escrita
-- A Edge Function utiliza a SUPABASE_SERVICE_ROLE_KEY interna, o que já garante
-- acesso administrativo direto para consultar e persistir dados nas tabelas:
--   - public.shared_finance_dashboards
--   - public.shared_tasks_hub
--   - public.shared_wishes_hub
--   - public.shared_market_lists

-- 2. Tabela de Auditoria e Logs Opcional do Bot
CREATE TABLE IF NOT EXISTS public.telegram_bot_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    telegram_user_id TEXT NOT NULL,
    action TEXT NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

ALTER TABLE public.telegram_bot_logs ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Service role can do everything on telegram_bot_logs" ON public.telegram_bot_logs;
CREATE POLICY "Service role can do everything on telegram_bot_logs"
ON public.telegram_bot_logs
FOR ALL
TO service_role
USING (true)
WITH CHECK (true);

-- ==============================================================================
-- 3. AGENDAMENTOS AUTOMÁTICOS (PUSH PROATIVO VIA PG_CRON E PG_NET)
-- ==============================================================================
-- Execute os comandos abaixo no SQL Editor do Supabase para ativar os resumos
-- diários matinal (08:00 BRT / 11:00 UTC) e noturno (21:00 BRT / 00:00 UTC).

-- Habilita extensões necessárias
CREATE EXTENSION IF NOT EXISTS pg_cron;
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Remove agendamentos antigos se já existirem (evita erro de duplicidade ou job inexistente)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'tessera_morning_briefing') THEN
        PERFORM cron.unschedule('tessera_morning_briefing');
    END IF;
    IF EXISTS (SELECT 1 FROM cron.job WHERE jobname = 'tessera_night_briefing') THEN
        PERFORM cron.unschedule('tessera_night_briefing');
    END IF;
END $$;

-- Agendamento 1: Resumo Matinal às 08:00 Horário de Brasília (11:00 UTC)
-- Envia clima, saldo livre e principais tarefas do dia
SELECT cron.schedule(
    'tessera_morning_briefing',
    '0 11 * * *',
    $$
    SELECT net.http_post(
        url := 'https://hyoveowiisbigcpxzoro.supabase.co/functions/v1/telegram-bot',
        headers := jsonb_build_object(
            'Content-Type', 'application/json'
        ),
        body := jsonb_build_object(
            'cron_event', 'morning_briefing'
        )
    );
    $$
);

-- Agendamento 2: Resumo Noturno às 21:00 Horário de Brasília (00:00 UTC)
-- Envia total gasto no dia, pendências de compras e lembrete para registrar despesas
SELECT cron.schedule(
    'tessera_night_briefing',
    '0 0 * * *',
    $$
    SELECT net.http_post(
        url := 'https://hyoveowiisbigcpxzoro.supabase.co/functions/v1/telegram-bot',
        headers := jsonb_build_object(
            'Content-Type', 'application/json'
        ),
        body := jsonb_build_object(
            'cron_event', 'night_briefing'
        )
    );
    $$
);

-- Para verificar os agendamentos ativos:
-- SELECT * FROM cron.job;

-- Para acompanhar o histórico de execuções do cron:
-- SELECT * FROM cron.job_run_details ORDER BY start_time DESC LIMIT 10;
