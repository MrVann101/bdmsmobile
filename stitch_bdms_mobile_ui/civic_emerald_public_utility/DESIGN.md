---
name: Civic Emerald Public Utility
colors:
  surface: '#fbf8ff'
  surface-dim: '#dad9e4'
  surface-bright: '#fbf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f2fd'
  surface-container: '#efecf8'
  surface-container-high: '#e9e7f2'
  surface-container-highest: '#e3e1ec'
  on-surface: '#1a1b23'
  on-surface-variant: '#3d4a42'
  inverse-surface: '#2f3038'
  inverse-on-surface: '#f1effa'
  outline: '#6d7a72'
  outline-variant: '#bccac0'
  surface-tint: '#006c4a'
  primary: '#006948'
  on-primary: '#ffffff'
  primary-container: '#00855d'
  on-primary-container: '#f5fff7'
  inverse-primary: '#68dba9'
  secondary: '#006c4e'
  on-secondary: '#ffffff'
  secondary-container: '#97f5cc'
  on-secondary-container: '#007353'
  tertiary: '#3f6353'
  on-tertiary: '#ffffff'
  tertiary-container: '#577c6b'
  on-tertiary-container: '#f5fff7'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#85f8c4'
  primary-fixed-dim: '#68dba9'
  on-primary-fixed: '#002114'
  on-primary-fixed-variant: '#005137'
  secondary-fixed: '#97f5cc'
  secondary-fixed-dim: '#7bd8b1'
  on-secondary-fixed: '#002115'
  on-secondary-fixed-variant: '#00513a'
  tertiary-fixed: '#c3ecd7'
  tertiary-fixed-dim: '#a8cfbc'
  on-tertiary-fixed: '#002115'
  on-tertiary-fixed-variant: '#294e3f'
  background: '#fbf8ff'
  on-background: '#1a1b23'
  surface-variant: '#e3e1ec'
typography:
  display-lg:
    fontFamily: Public Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Public Sans
    fontSize: 26px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Public Sans
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
  headline-sm:
    fontFamily: Public Sans
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Public Sans
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  body-sm:
    fontFamily: Public Sans
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 18px
  label-lg:
    fontFamily: Public Sans
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.01em
  label-md:
    fontFamily: Public Sans
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.02em
  label-sm:
    fontFamily: Public Sans
    fontSize: 11px
    fontWeight: '500'
    lineHeight: 14px
    letterSpacing: 0.03em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1rem
  margin: 1rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 0.75rem
  space-lg: 1rem
  space-xl: 1.5rem
---

## Brand & Style

This design system establishes an accessible, civic-grade interface for Philippine barangay local governance. The aesthetic pairs the systematic clarity of Material Design 3 with an authoritative, approachable public-sector tone. 

The primary persona encompasses local citizens—spanning varied age groups, literacy tiers, and smartphone proficiencies—transacting for official barangay clearances, lodging incident grievances, or broadcasting critical emergency alerts (calamity, medical, fire). 

The emotional signature is calming, transparent, and direct. Visual noise is minimized to prioritize cognitive ease under high-stress conditions (such as typhoons or disputes). Subtle structural lines, crisp outline iconography (Lucide / Material Symbols Outlined), and precise typographic hierarchy project institutional integrity without feeling bureaucratic or intimidating.

## Colors

The palette is engineered around civic dependability and strict WCAG AA/AAA legibility standards across outdoor tropical lighting conditions.

### Core Canvas & Structure
- **Canvas Background**: `#FCFCFC` — ultra-light neutral ground minimizing glare.
- **Surface Cards & Sheets**: `#FFFFFF` — clean contrast base for discrete information units.
- **Border / Divider**: `#E5E5E5` — structural boundaries without visual aggression.

### Text Hierarchy
- **Text Primary**: `#171717` — deep charcoal for titles, primary values, and body text.
- **Text Secondary**: `#52525B` — medium-slate tone for supporting details, timestamps, and metadata.
- **Text Muted / Placeholder**: `#A1A1AA` — inactive labels, non-critical trailing icons, and disabled states.

### Primary Brand Matrix
- **Primary**: `#059669` (Emerald 600) — key actions, active navigation icons, and system affirmation.
- **Primary Dark**: `#047857` (Emerald 700) — pressed/hover interactions, critical confirmations.
- **Primary Light Tint**: `#D1FAE5` (Emerald 100) — subtle surface selection fills, active tab highlights, key accent grounds.

### Administrative Status Indicators (Badges & Pills)
- **Pending / Reported**: Background `#FFFBEB`, Text `#B45309` (Amber tone for awaiting action).
- **Processing / Reviewing / Verified / Relief Dispatched**: Background `#EFF6FF`, Text `#1D4ED8` (Blue tone for operational progress).
- **Ready for Pickup**: Background `#ECFEFF`, Text `#0E7490` (Cyan tone for immediate document claim).
- **Completed / Resolved**: Background `#F0FDF4`, Text `#15803D` (Forest green tone for finalized processes).
- **Rejected / Dismissed / Overdue / High Priority Emergency**: Background `#FEF2F2`, Text `#B91C1C` (Crimson tone for alerts, denials, or immediate hazards).

## Typography

The typography is built entirely on `Public Sans`, an open-source, government-grade typeface designed specifically for institutional and public interfaces. It offers optimal reading rhythm, neutral optical character, and robust rendering across low-resolution mobile screens.

- **Numerals**: Tabular figures should be enabled for barangay reference IDs, official receipt numbers, and queue counts.
- **Bilingual Balance**: Filipino (Tagalog/Cebuano/Ilocano) and English document titles exhibit equal clarity, with generous line heights preventing crowding in descriptive text blocks.
- **Tracking**: Tight tracking on large headers enhances structural impact, while deliberate positive tracking on labels (`label-md`, `label-sm`) preserves legibility in all-caps status tags and chips.

## Layout & Spacing

The layout is anchored strictly to an Android mobile portrait baseline target (390 x 844dp), scaling fluidly across standard viewport widths (360dp to 428dp).

### Grid & Canvas Structure
- **Outer Canvas Margins**: Uniform 16dp (`1rem`) on horizontal mobile boundaries.
- **Internal Column Grid**: Single-column vertical flow with dynamic 2-column card grouping for quick utility shortcuts (e.g., Quick Services grid: Clearance, Blotter, Permit, Indigency).
- **Rhythm Multiplier**: An invariant 4dp / 8dp vertical baseline. All card padding, internal element separation, and stack spacing map directly to multiples of 4dp.

### Form Factor Considerations
- **Touch Target Envelopes**: Crucial for field use. All interactive icons, bottom navigation destinations, and buttons require a minimum 48x48dp hit box, irrespective of visual icon glyph size (20dp to 24dp).
- **Thumb Zone Optimization**: Primary trigger elements (e.g., "Request Certificate", "Submit Blotter", "Call Rescue 911") sit within the bottom 40% of the screen or pin to a persistent floating bottom dock.

## Elevation & Depth

To maintain high contrast and civic sobriety, this design system limits dramatic Drop Shadows and Neomorphism, favoring flat tonal surfaces bounded by low-contrast outlines.

### Elevation Levels
- **Level 0 (Flat Canvas)**: Pure background `#FCFCFC`. No elevation, non-interactive foundation.
- **Level 1 (Cards, List Containers, Input Surfaces)**: Surface `#FFFFFF` bordered by a crisp `1px solid #E5E5E5`. No drop shadow under resting states.
- **Level 2 (Active/Pressed Cards, Dropdown Panels)**: Surface `#FFFFFF` with a subtle ambient tint: `box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02)`.
- **Level 3 (Modal Sheets & Bottom Emergency Bars)**: Persistent bottom sheets use `box-shadow: 0 -4px 16px rgba(23, 23, 23, 0.06)`, framed with a top border `#E5E5E5`.
- **Emergency Priority Elevation**: Dedicated emergency SOS widgets float over content with `box-shadow: 0 4px 14px rgba(185, 28, 28, 0.25)`.

## Shapes

The geometric personality blends modern Android ergonomics with functional precision:

- **Cards & Surface Containers**: Standardized at `12dp` radius. This provides structural distinction without looking toy-like.
- **Action Buttons & Form Inputs**: Standardized at `8dp` radius. Ensures buttons communicate a reliable, stamp-like action state.
- **Status Badges & Filter Chips**: Standardized at full-pill (`9999dp`) radius to distinguish operational tags from clickable rectangular cards.
- **Modal Sheets**: Upper sheet corners conform to `16dp` radius, matching standard Material 3 ergonomics.

## Components

### Buttons
- **Primary Action**: Height 48dp, 8dp border radius, background `#059669`, text `#FFFFFF` (Public Sans Semibold 14dp). Active state `#047857`. Full horizontal width on bottom sheet forms.
- **Secondary / Outlined Action**: Height 48dp, 8dp border radius, background `#FFFFFF`, border `1px solid #E5E5E5`, text `#171717`. Active state background `#F4F4F5`.
- **Emergency Action Button (SOS)**: Full width, height 52dp, 8dp radius, background `#DC2626`, text `#FFFFFF`, leading alert icon. Active state `#B91C1C`.

### Form Input Fields
- **Container**: Minimum height 52dp, 8dp border radius, background `#FFFFFF`, border `1px solid #E5E5E5`.
- **Focus State**: Border shifts to `2px solid #059669` accompanied by an emerald-tinted outer ring (`rgba(5, 150, 105, 0.12)`).
- **Labels & Helpers**: Floating or stacked upper label in `#52525B` (`label-md`). Helper text underneath in `#A1A1AA`. Error message in `#B91C1C` with an error outline `#EF4444`.

### Status Badges & Chips
- **Geometry**: Height 24dp, rounded full (pill), internal padding `4dp 10dp`.
- **Typographic Treatment**: `label-sm` (Public Sans 11dp, Semibold, +0.03em tracking).
- **States**:
  - *Pending / Reported*: Background `#FFFBEB`, text `#B45309`.
  - *Processing / Reviewing / Verified*: Background `#EFF6FF`, text `#1D4ED8`.
  - *Ready for Pickup*: Background `#ECFEFF`, text `#0E7490`.
  - *Completed / Resolved*: Background `#F0FDF4`, text `#15803D`.
  - *Rejected / High-Priority / Alert*: Background `#FEF2F2`, text `#B91C1C`.

### Cards & Record Panels
- **Container**: Background `#FFFFFF`, 12dp border radius, border `1px solid #E5E5E5`, internal padding 16dp.
- **Document Request Card**: Displays transaction tracking code (monospace or tabular `Public Sans`), requested clearance type (e.g., *Barangay Indigency Certificate*), date requested, and right-aligned status badge.
- **Incident Blotter Card**: Header with blotter case number, short excerpt of grievance, timestamp, and verification badge.

### Lists & Navigation
- **List Rows**: Minimal 56dp height, dividers `1px solid #E5E5E5` inset by 16dp, trailing chevron indicator (`#A1A1AA`).
- **Bottom Navigation Bar**: Height 64dp, background `#FFFFFF`, top border `1px solid #E5E5E5`. 4 to 5 fixed destinations (Home, Services, Blotter/Complaints, Emergency, Profile). Active tab tint `#059669` with an indicator pill using `#D1FAE5`.