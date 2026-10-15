<template>
  <component :is="icons[name] || Info" :size="size" :stroke-width="strokeWidth"
    class="app-icon" :aria-hidden="label ? undefined : true" :aria-label="label || undefined"
    :role="label ? 'img' : undefined" focusable="false" />
</template>

<script setup>
import {
  ParkingSquare, CarFront, Building2, Home, ClipboardList, ClipboardCheck, FileText,
  FileCheck2, UserRound, UsersRound, Contact, BadgeCheck, CheckCircle2, XCircle,
  AlertTriangle, Clock3, Hourglass, ListOrdered, ShieldCheck, LockKeyhole, KeyRound,
  Moon, Sun, Bell, LogOut, ArrowRight, ChevronRight, ArrowLeft, ChevronDown, X,
  Info, Loader2, PencilLine, Wrench, Check, Circle, Star, MapPin, Send,
  CalendarClock, RefreshCw, Megaphone, Search, FileDown, Plus, Settings2,
  LayoutDashboard, Eye, Sparkles, FileSearch, FolderOpen, BarChart3, TrendingUp,
  Globe, Smartphone, Target, Zap, Upload, Download, HeartHandshake,
} from 'lucide-vue-next'

defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 20 },
  strokeWidth: { type: [Number, String], default: 1.8 },
  label: { type: String, default: '' },
})

const icons = {
  parking: ParkingSquare, car: CarFront, building: Building2, home: Home,
  clipboard: ClipboardList, 'clipboard-check': ClipboardCheck, file: FileText,
  'file-check': FileCheck2, user: UserRound, users: UsersRound, identity: Contact,
  verified: BadgeCheck, success: CheckCircle2, error: XCircle, warning: AlertTriangle,
  clock: Clock3, hourglass: Hourglass, queue: ListOrdered, shield: ShieldCheck,
  lock: LockKeyhole, key: KeyRound, moon: Moon, sun: Sun, bell: Bell, logout: LogOut,
  arrow: ArrowRight, chevron: ChevronRight, back: ArrowLeft, down: ChevronDown,
  close: X, info: Info, loading: Loader2, edit: PencilLine, wrench: Wrench,
  check: Check, circle: Circle, star: Star, location: MapPin, send: Send,
  calendar: CalendarClock, refresh: RefreshCw, announcement: Megaphone,
  search: Search, export: FileDown, plus: Plus, settings: Settings2,
  dashboard: LayoutDashboard, eye: Eye, sparkles: Sparkles, review: FileSearch,
  folder: FolderOpen, chart: BarChart3, trend: TrendingUp, globe: Globe,
  phone: Smartphone, target: Target, zap: Zap, upload: Upload, download: Download,
  relationship: HeartHandshake,
}
</script>

<style scoped>
.app-icon { display: inline-block; flex-shrink: 0; vertical-align: -0.15em; }
</style>
