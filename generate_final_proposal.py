import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def create_final_proposal_docx(output_path):
    doc = docx.Document()

    # Set 1-inch margins
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)
        
        # Add footer with page numbering and review note
        footer = section.footer
        f_p = footer.paragraphs[0]
        f_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        f_run = f_p.add_run("AMASAMYA Strategic Blueprint v2.1 | Platform & Academy Master Plan")
        f_run.font.name = 'Segoe UI'
        f_run.font.size = Pt(8.5)
        f_run.font.color.rgb = RGBColor(140, 150, 160)

    # Styling Palette
    NAVY_PRIMARY = RGBColor(11, 25, 44)       # #0B192C - Deep Navy Header
    BLUE_ACCENT = RGBColor(0, 114, 188)       # #0072BC - Primary Blue Accent
    TEXT_DARK = RGBColor(33, 37, 41)          # #212529 - Charcoal Text
    TEXT_MUTED = RGBColor(100, 110, 120)      # Muted Gray
    BG_LIGHT_ROW = "F8FAFC"
    BG_HEADER = "0B192C"
    WHITE = RGBColor(255, 255, 255)

    # Base Style
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Segoe UI'
    style_normal.font.size = Pt(10.5)
    style_normal.font.color.rgb = TEXT_DARK

    def set_cell_background(cell, fill_hex):
        tcPr = cell._tc.get_or_add_tcPr()
        shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
        tcPr.append(shd)

    def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
        tcPr = cell._tc.get_or_add_tcPr()
        tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
        tcPr.append(tcMar)

    def add_title(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(4)
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(22)
        run.font.bold = True
        run.font.color.rgb = NAVY_PRIMARY
        return p

    def add_subtitle(text):
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(14)
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(12)
        run.font.italic = True
        run.font.color.rgb = BLUE_ACCENT
        return p

    def add_meta_box(leaders_text, meta_text):
        tbl = doc.add_table(rows=1, cols=1)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        cell = tbl.rows[0].cells[0]
        set_cell_background(cell, "F1F5F9")
        set_cell_margins(cell, top=140, bottom=140, left=180, right=180)
        
        tcPr = cell._tc.get_or_add_tcPr()
        borders = parse_xml(f'<w:tcBorders {nsdecls("w")}><w:left w:val="single" w:sz="24" w:space="0" w:color="0072BC"/><w:top w:val="none"/><w:right w:val="none"/><w:bottom w:val="none"/></w:tcBorders>')
        tcPr.append(borders)

        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(3)
        r1 = p.add_run("Platform & Academy Leadership:\n")
        r1.font.bold = True
        r1.font.size = Pt(10)
        r1.font.color.rgb = NAVY_PRIMARY

        r2 = p.add_run(leaders_text + "\n\n")
        r2.font.size = Pt(9.5)
        r2.font.color.rgb = TEXT_DARK

        r3 = p.add_run(meta_text)
        r3.font.size = Pt(9)
        r3.font.italic = True
        r3.font.color.rgb = TEXT_MUTED

        doc.add_paragraph().paragraph_format.space_after = Pt(8)

    def add_h1(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(18)
        p.paragraph_format.space_after = Pt(6)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(15)
        run.font.bold = True
        run.font.color.rgb = NAVY_PRIMARY
        return p

    def add_h2(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(12)
        p.paragraph_format.space_after = Pt(4)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(12)
        run.font.bold = True
        run.font.color.rgb = BLUE_ACCENT
        return p

    def add_h3(text):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(8)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.keep_with_next = True
        run = p.add_run(text)
        run.font.name = 'Segoe UI'
        run.font.size = Pt(10.5)
        run.font.bold = True
        run.font.color.rgb = NAVY_PRIMARY
        return p

    def add_p(text, bold_prefix=None):
        p = doc.add_paragraph()
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(5)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = 'Segoe UI'
            r_pre.font.bold = True
            r_pre.font.color.rgb = NAVY_PRIMARY
        r_text = p.add_run(text)
        r_text.font.name = 'Segoe UI'
        r_text.font.color.rgb = TEXT_DARK
        return p

    def add_bullet(text, bold_prefix=None, indent_level=0):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(3)
        p.paragraph_format.line_spacing = 1.15
        if indent_level > 0:
            p.paragraph_format.left_indent = Inches(0.25 * indent_level)
        if bold_prefix:
            r_pre = p.add_run(bold_prefix)
            r_pre.font.name = 'Segoe UI'
            r_pre.font.bold = True
            r_pre.font.color.rgb = NAVY_PRIMARY
        r_text = p.add_run(text)
        r_text.font.name = 'Segoe UI'
        r_text.font.color.rgb = TEXT_DARK
        return p

    def add_callout(title, text):
        tbl = doc.add_table(rows=1, cols=1)
        tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
        cell = tbl.rows[0].cells[0]
        set_cell_background(cell, "F8FAFC")
        set_cell_margins(cell, top=100, bottom=100, left=140, right=140)
        
        tcPr = cell._tc.get_or_add_tcPr()
        borders = parse_xml(f'<w:tcBorders {nsdecls("w")}><w:left w:val="single" w:sz="18" w:space="0" w:color="0072BC"/><w:top w:val="none"/><w:right w:val="none"/><w:bottom w:val="none"/></w:tcBorders>')
        tcPr.append(borders)

        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(2)
        r1 = p.add_run(f"Note - {title}: ")
        r1.font.bold = True
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = BLUE_ACCENT

        r2 = p.add_run(text)
        r2.font.size = Pt(9.5)
        r2.font.color.rgb = TEXT_DARK
        doc.add_paragraph().paragraph_format.space_after = Pt(4)

    # --- DOCUMENT HEADER ---
    add_title("AMASAMYA: Inclusive Technology & Professional Learning Platform")
    add_subtitle("A Strategic Proposal & Master Execution Blueprint for Equal Opportunity in Tech, Language, and Career Empowerment")

    leaders_str = (
        "• Akhilesh Malani - Founder & Creator, AMASAMYA | Lead, School of Technology & Accessibility Engineering\n"
        "  (Enterprise Accessibility Architect with 16+ years experience across Google, Bank of Montreal, Dun & Bradstreet, Oracle; creator of AMASAMYA audit suite across Android, Web, and Extensions; author of VPAT 2.4, GIGW 3.0, and IS 17802 engines)\n\n"
        "• L. Subramani - Founding Academic Director, AMASAMYA Academy | Lead, School of English & Professional Communication\n"
        "  (Veteran Journalist with 28+ years experience, former Chief Copy Editor at Deccan Herald, acclaimed author of 'Lights Out', communications strategist & disability advocate)"
    )
    meta_str = "Document Version: 2.1 (Platform & Academy Master Plan) | Date: August 2026 | Distribution: Confidential / Internal"
    add_meta_box(leaders_str, meta_str)

    # --- SECTION 1: EXECUTIVE VISION ---
    add_h1("1. Executive Vision and Core Mission")
    add_p("The mission of the AMASAMYA platform is to create a barrier-free ecosystem where persons with disabilities and mainstream learners build professional English and accessibility engineering skills side by side, taught by educators who navigate the digital world using the exact same assistive technology stack as the learners.")
    add_p("AMASAMYA is founded and engineered by Akhilesh Malani as an overarching accessibility technology ecosystem. Within this ecosystem, AMASAMYA Academy is co-led in academic partnership with L. Subramani to provide rigorous, executive-level language and professional communication training.")
    add_p("Every learner completing the AMASAMYA curriculum leaves with two tangible, career-defining assets:")
    add_bullet(" Corporate-grade written and spoken communication ability tailored for multinational and executive workplaces.", bold_prefix="1. Executive-English Mastery:")
    add_bullet(" Verifiable credentials in accessibility engineering, standards compliance (WCAG 2.2, GIGW 3.0, IS 17802), or accessible content creation.", bold_prefix="2. Marketable Technical Identity:")

    # --- SECTION 2: MARKET LANDSCAPE & STRATEGIC GAP ---
    add_h1("2. Market Landscape & Strategic Gap Analysis")
    add_p("The four primary learner segments addressed by AMASAMYA are completely underserved on key functional dimensions by incumbent offerings:")

    table_gap = doc.add_table(rows=1, cols=3)
    table_gap.alignment = WD_TABLE_ALIGNMENT.CENTER
    hdr = table_gap.rows[0].cells
    hdr[0].text = "Learner Segment"
    hdr[1].text = "Incumbent Solutions & Limitations"
    hdr[2].text = "The AMASAMYA Advantage"
    
    col_widths = [Inches(1.8), Inches(2.5), Inches(2.2)]
    for i, cell in enumerate(hdr):
        cell.width = col_widths[i]
        set_cell_background(cell, BG_HEADER)
        set_cell_margins(cell, top=100, bottom=100, left=120, right=120)
        p = cell.paragraphs[0]
        for r in p.runs:
            r.font.bold = True
            r.font.color.rgb = WHITE
            r.font.size = Pt(9.5)

    gaps = [
        ("Blind Adults preparing for corporate English roles",
         "Duolingo, Grammarly, ELSA: Heavily visual (drag-and-drop, image cues, video-first). Focus on casual dialogue; none deliver corporate-register writing or screen-reader usability end to end.",
         "Audio-first, screen-reader native pedagogy curated by L. Subramani. Focuses directly on business writing, nuance, and verbal workplace fluency."),
        ("Blind Educators authoring accessible courses",
         "Canvas, Moodle, Coursera authoring suites: Authoring dashboards are visual-first. Blind educators cannot independently build, organize, or publish courses without sighted assistance.",
         "Inclusive audio-first authoring dashboard. Non-visual workflow with polite live region announcements, keyboard shortcuts, and simple quiz builders."),
        ("Accessibility Engineers seeking Indian standards training",
         "Deque University, W3C WAI: Prohibitively priced ($150 to $300 USD per course). English-only; zero coverage of GIGW 3.0, IS 17802, or SEBI accessibility mandates.",
         "Affordable domestic pricing in INR, integrated audit tool exercises, full coverage of Indian national standards alongside global WCAG 2.2 benchmarks."),
        ("Indian Government & Enterprise DEI Programs",
         "DIKSHA, SWAYAM, Sugamya Pustakalaya: High volume of content, but relies heavily on inaccessible PDFs, image-only quizzes, and uncaptioned video. No interactive labs.",
         "Certified accessible LMS environment, interactive technical labs, and verifiable compliance reporting (VPAT 2.4 ACR).")
    ]

    for seg, inc, adv in gaps:
        row = table_gap.add_row().cells
        row[0].text = seg
        row[1].text = inc
        row[2].text = adv
        for i, cell in enumerate(row):
            cell.width = col_widths[i]
            set_cell_background(cell, BG_LIGHT_ROW if gaps.index((seg, inc, adv)) % 2 == 0 else "FFFFFF")
            set_cell_margins(cell, top=80, bottom=80, left=100, right=100)
            p = cell.paragraphs[0]
            for r in p.runs:
                r.font.size = Pt(9)
                r.font.color.rgb = TEXT_DARK

    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # --- SECTION 3: YEAR ONE SCOPE VS YEAR TWO ---
    add_h1("3. What We Build in Year One vs. What We Deliberately Defer")
    add_p("To ensure flawless execution and avoid over-engineering, Version 2.1 strictly isolates Year One around core pedagogic proof and accessible infrastructure, deferring complex automation engines to Year Two.")

    add_h2("Year-One Scope (v1): Core Pedagogical & Delivery Engines")
    add_p("Pillar 1: School of English and Professional Communication (Led by L. Subramani)", bold_prefix="• ")
    add_bullet(" Phonics, sentence construction, tense consistency, vocabulary building, screen-reader friendly grammar drills, listening comprehension.", bold_prefix="Track A: English Foundations:", indent_level=1)
    add_bullet(" High-impact business emails, status reports, technical documentation, copyediting, and jargon elimination.", bold_prefix="Track B: Professional & Corporate Writing:", indent_level=1)
    add_bullet(" Meeting leadership, public speaking, voice modulation, interview mastery, and effective communication pace.", bold_prefix="Track C: Executive Verbal Fluency:", indent_level=1)
    add_bullet(" Long-form feature writing, opinion pieces, accessible alt-text authoring, and digital advocacy storytelling.", bold_prefix="Track D: Journalism & Content Craft:", indent_level=1)

    add_p("Pillar 4a: Accessible LMS Learner Surface & Blind-Educator Audio-First Authoring", bold_prefix="• ")
    add_bullet(" Full keyboard-only navigation, high-contrast palette, ARIA live region optimization, audio-first lesson streaming, and screen-reader optimized reading views.", bold_prefix="Learner Surface:", indent_level=1)
    add_bullet(" Audio recording, structured lesson upload, and quiz authoring tool allowing a blind educator to independently create and publish a complete course without sighted assistance.", bold_prefix="Educator Authoring Tool:", indent_level=1)

    add_callout("Pedagogical Bridge (The Audio-to-Text Workflow)", 
                "While instruction is delivered audio-first, writing mastery requires granular text interaction. Track B and D exercises follow a tight loop: (1) Listen to audio micro-lecture -> (2) Navigate accessible markdown sample text -> (3) Draft response in an accessible web editor -> (4) Receive structured rubric feedback from peer review circles and instructor office hours.")

    add_h2("Year-Two Scope (v2): Scaled Automation & Interactive Sandboxes")
    add_bullet(" Automated readability, grammar, tone, and clarity evaluation engine. Deferred to Year Two because the underlying pedagogical rules and rubrics established by L. Subramani in Year One must be validated first.", bold_prefix="Pillar 3: AMASAMYA CopyAudit Engine:")
    add_bullet(" Live browser code sandbox providing instant screen-reader utterance simulations. Deferred to Year Two because Year One engineering tracks can be effectively delivered via existing extension audits, lectures, and live code remediation exercises.", bold_prefix="Pillar 2b: AMASAMYA CodeLab:")

    add_h2("Out of Year-One Scope Entirely")
    add_bullet(" Requires native-speaker instructors, script-specific screen reader QA, and regional localization. Planned as a v3 milestone post Year-One traction.", bold_prefix="Indic Regional Language Learning Paths:")
    add_bullet(" Enterprise procurement cycles span 6 to 18 months in India. Year One financial sustainability does not rely on enterprise closures; corporate outreach begins in Month 10 for Year Two delivery.", bold_prefix="Full B2B Enterprise Sales Motion:")

    # --- SECTION 4: 12-MONTH ROADMAP WITH GATE CONDITIONS ---
    add_h1("4. Twelve-Month Roadmap with Explicit Numeric Gate Conditions")
    add_p("Progression between phases is governed by strict numeric gate conditions. If a gate is not met, the leadership will iterate the current curriculum and product before investing capital in subsequent phases.")

    add_h2("Phase 1 (Months 1 to 4): Foundation, First Cohort & Curriculum Lock")
    add_bullet(" Curriculum outline for Tracks A and B finalized, reviewed, and recorded by L. Subramani (8 initial audio-first lessons).")
    add_bullet(" Accessible LMS learner portal deployed; educator authoring tool at functional MVP.")
    add_bullet(" Enrolment of Cohort 1: 30 verified blind learners via AMASAMYA extension link and partnered disability organizations (with advanced placement testing for learners skipping Track A into Track B).")
    add_p("First cohort achieves at least 40% retention (12 of 30 learners reaching Level 2 completion) and written curriculum feedback submitted by at least 15 learners.", bold_prefix="Gate to Phase 2: ")

    add_h2("Phase 2 (Months 5 to 8): Tracks C & D, Paid Tier & Peer Review Circles")
    add_bullet(" Tracks C (Executive Verbal Fluency) and D (Journalism & Content Craft) recorded and published.")
    add_bullet(" Launch of individual paid subscription tier at introductory pricing (INR 999 per month).")
    add_bullet(" Enrolment of Cohort 2: Target 100 learners across free and paid tiers.")
    add_bullet(" Implementation of moderated Peer Writing Circles to scale assignment reviews without bottlenecking instructor bandwidth.")
    add_bullet(" Educator Co-Design Pilot: 3 external blind educators onboarded to test and publish through the authoring tool.")
    add_p("At least 20 active paying learners and at least 1 external blind educator successfully publishes a course through the platform.", bold_prefix="Gate to Phase 3: ")

    add_h2("Phase 3 (Months 9 to 12): Institutional Scale & Educator Platform Launch")
    add_bullet(" Corporate DEI and CSR outreach: 5 qualified corporate discussions; initiate 1 pilot program.")
    add_bullet(" Authoring platform moves out of beta and opens to verified community educators.")
    add_bullet(" Institutional partnership discussions initiated with 3 Indian universities and 2 blind schools.")
    add_bullet(" Technical and pedagogical scoping for Year Two engines (CopyAudit & CodeLab).")
    add_p("100 active paying learners OR 1 signed enterprise pilot OR 2 signed institutional MOUs. Any one of the three triggers Year Two expansion.", bold_prefix="Gate to Year Two: ")

    # --- SECTION 5: MONETISATION MODEL ---
    add_h1("5. Monetisation Model & Financial Sustainability")
    add_p("The platform combines community affordability with enterprise and institutional revenue streams. The foundational track remains permanently free to protect community access.")
    add_bullet(" Full Track A (Foundations) plus Module 1 of Track B. Fully accessible LMS access with no time limit. Subsidized by paid tiers and corporate sponsorships.", bold_prefix="• Free Community Tier:")
    add_bullet(" Complete Tracks A through D plus certification pathway plus peer circle participation. INR 999 per month or INR 8,999 per year (deliberately priced below foreign platforms like Deque to suit Indian professionals).", bold_prefix="• Individual Paid Tier:")
    add_bullet(" Bulk licenses for corporate DEI hiring pipelines and accessibility engineering upskilling. INR 2 to 5 Lakhs per organization per year (includes up to 25 learners plus hiring pipeline visibility).", bold_prefix="• Corporate / CSR Sponsor-a-Scholar Tier:")
    add_bullet(" Discounted, cost-recovery volume pricing for universities, special schools, and NGO training centers.", bold_prefix="• Institutional Tier:")
    add_bullet(" Pursue non-dilutive assistive technology and disability empowerment grants (e.g. MSJE, international inclusion foundations) to underwrite regional language modules in Year Two.", bold_prefix="• Grant Funding:")

    # --- SECTION 6: GOVERNANCE, IP & LEGAL ENTITY ---
    add_h1("6. Governance, Intellectual Property, and Leadership Structure")
    add_bullet(" AMASAMYA is owned and founded by Akhilesh Malani. The legal entity holding the master brand, website, and technology is owned 100% by Akhilesh Malani.", bold_prefix="• Master Entity & Ownership:")
    add_bullet(" All AMASAMYA software IP (browser extensions, Android app, web audit engine, VPAT ACR generator, CodeLab, and platform source code) remains the exclusive, undiluted property of Akhilesh Malani.", bold_prefix="• Software IP Ownership:")
    add_bullet(" L. Subramani joins as Founding Academic Director of AMASAMYA Academy and Academic Lead for the School of English and Professional Communication. Course content authored by Subramani is licensed to AMASAMYA under a primary academic partnership.", bold_prefix="• Academy Leadership & Academic Partnership:")
    add_bullet(" Revenue generated from Academy student subscriptions, corporate DEI communication licenses, and institutional curriculum packages is shared equitably between Akhilesh Malani and L. Subramani following direct platform infrastructure expenses.", bold_prefix="• Commercial Revenue Sharing:")
    add_bullet(" Course completion certificates issued by the Academy are co-signed by both Akhilesh Malani and L. Subramani, providing learners with combined validation from enterprise accessibility engineering and senior journalism.", bold_prefix="• Joint Certification Credibility:")

    # --- SECTION 7: REGULATORY POSTURE ---
    add_h1("7. Regulatory Posture & Trust Compliance")
    add_bullet(" Enrolment in Year One is restricted to adults (18+) to eliminate minor-consent regulatory overhead. Full digital consent and explicit data privacy terms implemented on the portal.", bold_prefix="• DPDP Act 2023 Compliance:")
    add_bullet(" Marketing will strictly position credentials as 'Certificates of Professional Completion' rather than university degrees or diplomas, maintaining compliance with UGC and AICTE ed-tech guidelines.", bold_prefix="• Credential Positioning:")
    add_bullet(" Complete compliance with RPwD Act 2016, WCAG 2.2 AA, and GIGW 3.0. The platform's own Accessibility Conformance Report (VPAT 2.4) will be published openly in Phase 1 as an unassailable trust signal.", bold_prefix="• Accessibility Assurance:")

    # --- SECTION 8: EXTENDED TEAM & BUDGET ---
    add_h1("8. Year-One Extended Team & Budget Estimates")
    add_p("Year One operates on a lean, contractor-based model without full-time payroll overhead:")
    add_bullet(" Partnership agreement, licensing draft, DPDP consent review (~INR 1,00,000).", bold_prefix="• Legal & Compliance Counsel:")
    add_bullet(" Professional audio mastering and acoustic balancing for the first 20 core audio lessons (~INR 50,000 to 1,00,000).", bold_prefix="• Accessible Audio Production Engineer:")
    add_bullet(" External third-party blind accessibility auditor to conduct independent monthly conformance audits (~INR 15,000 per audit cycle).", bold_prefix="• Independent Accessibility QA Reviewer:")
    add_bullet(" Part-time manager to guide peer writing circles, learner onboarding, and community forums starting in Phase 2 (~INR 25,000 to 40,000 per month).", bold_prefix="• Community & Peer Circle Lead:")
    add_bullet(" Honorarium for first blind educator to author and publish a 20-lesson co-design test course in Phase 2 (~INR 75,000).", bold_prefix="• Contract Educator Co-Design Lead:")

    # --- SECTION 9: RISK REGISTER ---
    add_h1("9. Risk Register & Proactive Mitigations")
    add_bullet(" Mid-Phase 1 qualitative check-ins; introduce buddy and peer accountability systems to catch curriculum friction early.", bold_prefix="• Risk: Learner Drop-off Below 40% Gate | Mitigation:")
    add_bullet(" Keep Phase 1 tool constrained to a clean, non-visual audio recorder plus MCQ/short-answer quiz builder before expanding features.", bold_prefix="• Risk: Educator Authoring Tool UX Friction | Mitigation:")
    add_bullet(" Combine extension marketing with targeted outreach via National Association for the Blind (NAB), XRCVC, and disability networks.", bold_prefix="• Risk: Sourcing Phase 1 Learners | Mitigation:")
    add_bullet(" Introduce structured self-evaluation rubrics and peer review circles, keeping instructor review focused on capstone milestone submissions.", bold_prefix="• Risk: Instructor Feedback Scalability | Mitigation:")
    add_bullet(" Maintain documented curricula and operational playbooks from Day 1; designate trusted domain advisors per pillar.", bold_prefix="• Risk: Founder Key-Person Dependency | Mitigation:")

    # --- SECTION 10: DISCUSSION PROMPTS FOR L. SUBRAMANI ---
    add_h1("10. Discussion Prompts for L. Subramani (Feedback & Alignment)")
    add_p("To finalize Phase 1 curriculum production and educator workflows, your specific insights on the following five focal points are requested:")

    add_h2("Prompt 1: Curriculum Sequencing & Fast-Track Assessment")
    add_p("Does the proposed four-track structure (Foundations -> Corporate Writing -> Executive Verbal Fluency -> Journalism/Advocacy) align with your pedagogical approach? How should we structure an entry-level assessment that allows advanced screen-reader users to skip Track A and enter directly into Track B?")

    add_h2("Prompt 2: Audio-First Lesson Length & Pedagogical Structure")
    add_p("For visually impaired adults learning professional English, what lesson duration and cadence have proven most effective in your experience? (e.g. 5-minute focused micro-lessons with drills vs. 20-minute comprehensive lecture masterclasses).")

    add_h2("Prompt 3: The Audio-to-Text Writing Bridge")
    add_p("When teaching corporate writing (emails, summaries, reports), how should audio instruction connect to text editing for screen reader users? What specific text exercises (e.g. editing intentional errors, rewriting ambiguous memos) should students complete?")

    add_h2("Prompt 4: Copyediting Rubrics & Early CopyAudit Heuristics")
    add_p("When you copyedit student work, what are the top 3 to 5 common errors or habits you look for first (e.g. passive voice overuse, vague phrasing, structural clutter)? Defining these rules will serve as the exact blueprint for our Year Two CopyAudit engine.")

    add_h2("Prompt 5: Peer Circles & Scalable Evaluation Mechanics")
    add_p("How can we best structure peer writing circles so that learners give and receive meaningful feedback on business writing without requiring you to manually line-edit every submission as cohorts expand?")

    # --- SECTION 11: IMMEDIATE NEXT STEPS ---
    add_h1("11. Immediate Next Steps & Phase 1 Kick-Off")
    add_bullet(" L. Subramani reviews this Version 2.1 blueprint and shares feedback on Section 10 prompts.", bold_prefix="1. Leadership Alignment:")
    add_bullet(" Finalize syllabus and scripts for the initial 8 audio lessons across Tracks A and B.", bold_prefix="2. Curriculum Lock:")
    add_bullet(" Deploy MVP student portal, audio player, and educator authoring interface on the web.", bold_prefix="3. Web Platform MVP Deployment:")
    add_bullet(" Open applications for Cohort 1 (30 verified blind learners) and kick off Phase 1.", bold_prefix="4. Cohort 1 Enrolment:")

    doc.save(output_path)
    print(f"Document successfully created at: {output_path}")

if __name__ == "__main__":
    out_docx = r"C:\Users\akhi_\antigravity\focused-fermi\AMASAMYA_Strategic_Proposal_v2.1.docx"
    create_final_proposal_docx(out_docx)
