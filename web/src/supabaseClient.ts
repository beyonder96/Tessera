import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL || 'https://hyoveowiisbigcpxzoro.supabase.co'
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh5b3Zlb3dpaXNiaWdjcHh6b3JvIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODY5MTg2OTcsImV4cCI6MjEwMjQ5NDY5N30.FFHC_1r0d2buwOgElxp99RP10NrmjrOiE74F0zTW1T4'

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  realtime: {
    params: {
      eventsPerSecond: 10,
    },
  },
})
