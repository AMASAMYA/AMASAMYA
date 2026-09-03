import os
import docx
from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.style import WD_STYLE_TYPE
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn

def add_hyperlink(paragraph, url, text, color="0F4C81", underline=True):
    """
    Adds an accessible hyperlink to a paragraph.
    """
    # This gets access to the document.xml.rels file and gets a new relation id value
    part = paragraph.part
    r_id = part.relate_to(url, docx.opc.constants.RELATIONSHIP_TYPE.HYPERLINK, is_external=True)

    # Create the w:hyperlink tag and add needed values
    hyperlink = parse_xml(f'<w:hyperlink {nsdecls("w")} r:id="{r_id}" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>')

    # Create a w:r tag
    new_run = parse_xml(f'<w:r {nsdecls("w")}/>')
    rPr = parse_xml(f'<w:rPr {nsdecls("w")}/>')

    if color:
        c = parse_xml(f'<w:color {nsdecls("w")} w:val="{color}"/>')
        rPr.append(c)

    if underline:
        u = parse_xml(f'<w:u {nsdecls("w")} w:val="single"/>')
        rPr.append(u)

    new_run.append(rPr)
    new_run.text = text
    hyperlink.append(new_run)
    paragraph._p.append(hyperlink)

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}><w:top w:w="{top}" w:type="dxa"/><w:bottom w:w="{bottom}" w:type="dxa"/><w:left w:w="{left}" w:type="dxa"/><w:right w:w="{right}" w:type="dxa"/></w:tcMar>')
    tcPr.append(tcMar)

def add_bottom_border(paragraph, color="0F4C81", size="12"):
    pPr = paragraph._p.get_or_add_pPr()
    pBdr = parse_xml(f'<w:pBdr {nsdecls("w")}><w:bottom w:val="single" w:sz="{size}" w:space="4" w:color="{color}"/></w:pBdr>')
    pPr.append(pBdr)

def create_accessible_cv(output_path):
    doc = Document()

    # Set Document Properties (Essential for Accessibility / WCAG & PDF/UA compliance)
    core_props = doc.core_properties
    core_props.title = "Akhilesh Malani - Curriculum Vitae"
    core_props.author = "Akhilesh Malani"
    core_props.subject = "Lead Accessibility Test Architect & SME CV"
    core_props.keywords = "Accessibility Architect, WCAG 2.2, WAI-ARIA, Screen Readers, Section 508, Digital Inclusion"
    core_props.comments = "Accessible CV of Akhilesh Malani - Lead Accessibility Test Architect"

    # Set standard page margins (1 inch all around)
    sections = doc.sections
    for s in sections:
        s.top_margin = Inches(0.75)
        s.bottom_margin = Inches(0.75)
        s.left_margin = Inches(0.8)
        s.right_margin = Inches(0.8)

    # Set base styles
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Calibri'
    style_normal.font.size = Pt(10.5)
    style_normal.font.color.rgb = RGBColor(0x1F, 0x29, 0x37) # Dark neutral (#1F2937) for high contrast
    style_normal.paragraph_format.line_spacing = 1.15
    style_normal.paragraph_format.space_after = Pt(3)
    style_normal.paragraph_format.space_before = Pt(0)

    # Colors
    NAVY = RGBColor(0x0F, 0x3D, 0x6E) # Primary accent (#0F3D6E)
    SLATE = RGBColor(0x33, 0x41, 0x55) # Subheading (#334155)
    BODY = RGBColor(0x1F, 0x29, 0x37)
    ACCENT_HEX = "0F3D6E"

    # ----------------------------------------------------
    # HEADER / TITLE
    # ----------------------------------------------------
    p_name = doc.add_paragraph()
    p_name.paragraph_format.space_before = Pt(0)
    p_name.paragraph_format.space_after = Pt(2)
    run_name = p_name.add_run("AKHILESH MALANI")
    run_name.font.name = 'Calibri'
    run_name.font.size = Pt(22)
    run_name.font.bold = True
    run_name.font.color.rgb = NAVY

    p_title = doc.add_paragraph()
    p_title.paragraph_format.space_before = Pt(0)
    p_title.paragraph_format.space_after = Pt(6)
    run_title = p_title.add_run("Lead Accessibility Test Architect | Digital Inclusion & Assistive Technology SME")
    run_title.font.name = 'Calibri'
    run_title.font.size = Pt(11.5)
    run_title.font.bold = True
    run_title.font.color.rgb = SLATE

    # Contact Info line 1: Location & Phone & Email
    p_contact1 = doc.add_paragraph()
    p_contact1.paragraph_format.space_before = Pt(0)
    p_contact1.paragraph_format.space_after = Pt(2)
    
    r = p_contact1.add_run("Location: ")
    r.font.bold = True
    p_contact1.add_run("Chennai / Thiruninravur, Tamil Nadu – 602024, India  |  ")
    
    r = p_contact1.add_run("Phone: ")
    r.font.bold = True
    p_contact1.add_run("+91-900-053-1333  |  ")

    r = p_contact1.add_run("Email: ")
    r.font.bold = True
    add_hyperlink(p_contact1, "mailto:akhilesh.malani@gmail.com", "akhilesh.malani@gmail.com", color=ACCENT_HEX)

    # Contact Info line 2: Website, LinkedIn, AMASAMYA
    p_contact2 = doc.add_paragraph()
    p_contact2.paragraph_format.space_before = Pt(0)
    p_contact2.paragraph_format.space_after = Pt(8)

    r = p_contact2.add_run("Website: ")
    r.font.bold = True
    add_hyperlink(p_contact2, "https://www.akhileshmalani.com", "akhileshmalani.com", color=ACCENT_HEX)
    p_contact2.add_run("  |  ")

    r = p_contact2.add_run("LinkedIn: ")
    r.font.bold = True
    add_hyperlink(p_contact2, "https://www.linkedin.com/in/akhilesh-malani/", "linkedin.com/in/akhilesh-malani", color=ACCENT_HEX)
    p_contact2.add_run("  |  ")

    r = p_contact2.add_run("Project: ")
    r.font.bold = True
    add_hyperlink(p_contact2, "https://akhileshmalani.com/amasamya", "AMASAMYA Toolkit", color=ACCENT_HEX)

    add_bottom_border(p_contact2, color="CBD5E1", size="8")

    # Helper for Section Headings (Heading 1)
    def add_section_heading(title_text):
        h = doc.add_heading(level=1)
        h.paragraph_format.space_before = Pt(12)
        h.paragraph_format.space_after = Pt(4)
        h.paragraph_format.keep_with_next = True
        run = h.add_run(title_text)
        run.font.name = 'Calibri'
        run.font.size = Pt(13)
        run.font.bold = True
        run.font.color.rgb = NAVY
        add_bottom_border(h, color=ACCENT_HEX, size="10")
        return h

    # Helper for Subsection Headings (Heading 2)
    def add_subsection_heading(title_text, subtitle_text=None, date_text=None):
        h = doc.add_heading(level=2)
        h.paragraph_format.space_before = Pt(8)
        h.paragraph_format.space_after = Pt(2)
        h.paragraph_format.keep_with_next = True
        
        run = h.add_run(title_text)
        run.font.name = 'Calibri'
        run.font.size = Pt(11)
        run.font.bold = True
        run.font.color.rgb = NAVY

        if subtitle_text:
            r_sub = h.add_run(f"  |  {subtitle_text}")
            r_sub.font.name = 'Calibri'
            r_sub.font.size = Pt(10.5)
            r_sub.font.bold = False
            r_sub.font.italic = True
            r_sub.font.color.rgb = SLATE

        if date_text:
            p_date = doc.add_paragraph()
            p_date.paragraph_format.space_before = Pt(0)
            p_date.paragraph_format.space_after = Pt(3)
            p_date.paragraph_format.keep_with_next = True
            r_d = p_date.add_run(date_text)
            r_d.font.name = 'Calibri'
            r_d.font.size = Pt(9.5)
            r_d.font.bold = True
            r_d.font.color.rgb = SLATE
        return h

    # Helper for Bullet item
    def add_bullet(p_or_text, bold_prefix=None, rest_text=None):
        p = doc.add_paragraph(style='List Bullet')
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(2.5)
        p.paragraph_format.line_spacing = 1.15
        if bold_prefix:
            r_bold = p.add_run(bold_prefix)
            r_bold.font.name = 'Calibri'
            r_bold.font.size = Pt(10)
            r_bold.font.bold = True
            r_bold.font.color.rgb = BODY
        if rest_text:
            r_rest = p.add_run(rest_text)
            r_rest.font.name = 'Calibri'
            r_rest.font.size = Pt(10)
            r_rest.font.color.rgb = BODY
        return p

    # ----------------------------------------------------
    # 1. PROFESSIONAL SUMMARY
    # ----------------------------------------------------
    add_section_heading("Professional Summary")
    p_sum1 = doc.add_paragraph()
    p_sum1.paragraph_format.space_after = Pt(4)
    p_sum1.add_run(
        "Results-driven Lead Accessibility Architect and Subject Matter Expert (SME) with 16+ years of specialized "
        "experience spearheading digital accessibility (a11y), inclusive UX architecture, and global regulatory compliance across "
        "enterprise web, native mobile (iOS/Android), and digital document platforms. Combines 16+ years of daily lived experience "
        "as a blind professional using screen readers with deep engineering acumen to identify critical barriers, focus traps, "
        "and complex interaction flaws that automated scanning tools miss."
    )
    p_sum2 = doc.add_paragraph()
    p_sum2.paragraph_format.space_after = Pt(4)
    p_sum2.add_run(
        "Proven track record leading high-impact accessibility initiatives for Tier-1 global organizations—including "
    )
    r = p_sum2.add_run("Bank of Montreal, Dun & Bradstreet, Google (Google Play & Core Products), and Oracle Financial Services. ")
    r.font.bold = True
    p_sum2.add_run(
        "Recognized creator of AMASAMYA (open-source WCAG 2.2 audit toolkit and Chrome extension). Recipient of Tech Mahindra's "
        "ACE Award 2023 for outstanding delivery on a global accessibility program, and recognized at WWW2011 as Youngest Web Researcher."
    )

    # ----------------------------------------------------
    # 2. CORE COMPETENCIES & TECHNICAL SKILLS
    # ----------------------------------------------------
    add_section_heading("Core Competencies & Technical Skills")
    
    add_bullet(None, "Accessibility Standards & Frameworks: ", 
               "WCAG 2.0 / 2.1 / 2.2 (Levels A, AA, AAA), WAI-ARIA 1.2/1.3 Authoring Practices, Section 508 (US Rehabilitation Act), Americans with Disabilities Act (ADA Title III), EN 301 549 (EU Accessibility Mandate), PDF/UA (ISO 14289), AODA, GIGW 3.0, RPwD Act 2016, SEBI Digital Accessibility Mandate.")
    
    add_bullet(None, "Screen Readers & Assistive Technologies (AT): ", 
               "JAWS, NVDA, Apple VoiceOver (macOS & iOS), Google TalkBack (Android), Orca (Linux), Windows Narrator, ChromeVox, Dragon NaturallySpeaking (Speech Recognition).")
    
    add_bullet(None, "Assistive Hardware & Input Methods: ", 
               "Freedom Scientific Focus 40 Blue (Refreshable Braille Display), switch access devices, specialized keyboard-only navigation workflows, high-contrast & screen magnification testing.")
    
    add_bullet(None, "Auditing, Strategy & Remediation Architecture: ", 
               "Accessibility Conformance Reports (ACR based on VPAT 2.5), Shift-Left SDLC Integration, Technical Remediation Playbooks for Front-End Engineers, Design System & Component Architecture (complex data visualization, charts, dynamic modals, multi-step transaction flows), automated test script strategy & site crawl engines.")
    
    add_bullet(None, "Operating Systems & Platforms: ", 
               "Windows, macOS, Linux, iOS, Android, Cloud & SaaS environments, Microsoft Office Suite, PDF Accessibility tools.")
    
    add_bullet(None, "Leadership & Enablement: ", 
               "Leading diverse testing teams (including Persons with Disabilities), Cross-functional stakeholder management, Technical mentorship for engineering & UI/UX teams, Accessibility Champions programs, Executive workshops.")

    # ----------------------------------------------------
    # 3. PROFESSIONAL EXPERIENCE
    # ----------------------------------------------------
    add_section_heading("Professional Experience")

    # Virtusa
    add_subsection_heading("Virtusa Systems India Pvt. Ltd.", "Accessibility Test Architect", "March 2024 – Present | Chennai, India")
    
    p_bmo = doc.add_paragraph()
    p_bmo.paragraph_format.space_before = Pt(2)
    p_bmo.paragraph_format.space_after = Pt(2)
    p_bmo.paragraph_format.keep_with_next = True
    r = p_bmo.add_run("Client: Bank of Montreal (OLBB – Online Banking for Business) | March 2025 – Present")
    r.font.bold = True
    r.font.color.rgb = SLATE

    add_bullet(None, "Lead Accessibility Architect: ", "Serve as the Lead Accessibility Test Architect governing end-to-end accessibility evaluation and architectural remediation across BMO's customer-facing web applications, native mobile apps (iOS/Android), and transactional digital documents.")
    add_bullet(None, "Measurable Defect Reduction: ", "Spearheaded a targeted remediation roadmap and developer guidance framework that reduced critical accessibility defects by 73% across two consecutive major release cycles.")
    add_bullet(None, "WCAG 2.2 & Document Compliance: ", "Enforced strict compliance with WCAG 2.2 Level AA standards. Led both automated and manual validation initiatives for high-volume digital statements and PDFs, ensuring full PDF/UA and WCAG alignment.")
    add_bullet(None, "VPAT 2.5 & Technical Playbooks: ", "Standardized the creation and governance of Accessibility Conformance Reports (ACR based on VPAT 2.5) and authored comprehensive cross-platform technical remediation playbooks for development squads.")

    p_dnb = doc.add_paragraph()
    p_dnb.paragraph_format.space_before = Pt(4)
    p_dnb.paragraph_format.space_after = Pt(2)
    p_dnb.paragraph_format.keep_with_next = True
    r = p_dnb.add_run("Client: Dun & Bradstreet | April 2024 – February 2025")
    r.font.bold = True
    r.font.color.rgb = SLATE

    add_bullet(None, "Lead Accessibility Consultant: ", "Acted as Lead Accessibility SME for Dun & Bradstreet's flagship Risk Analytics web application, guaranteeing full compliance with WCAG 2.2 Level AA.")
    add_bullet(None, "Complex UI & ARIA Implementation: ", "Architected expert-level WAI-ARIA implementation strategies for intricate data visualization components, interactive charting widgets, and risk analytics dashboards.")
    add_bullet(None, "Shift-Left Champions Program: ", "Established an 'Accessibility Champions' program across design and development squads, providing technical mentorship to UI/UX designers and engineers to resolve accessibility defects early in the SDLC.")

    # Tech Mahindra
    add_subsection_heading("Tech Mahindra", "Accessibility Test Lead & SME", "November 2019 – March 2024 | Hyderabad / Chennai, India")
    
    p_tm_client = doc.add_paragraph()
    p_tm_client.paragraph_format.space_before = Pt(2)
    p_tm_client.paragraph_format.space_after = Pt(2)
    p_tm_client.paragraph_format.keep_with_next = True
    r = p_tm_client.add_run("Client: Google India")
    r.font.bold = True
    r.font.color.rgb = SLATE

    add_bullet(None, "Team Leadership: ", "Led, managed, and mentored a high-performing accessibility testing team of 15 engineers (including Persons with Disabilities - PwD) in executing Google's rigorous global accessibility standards.")
    add_bullet(None, "Engineering Enablement (12 Teams): ", "Designed and delivered practical, hands-on accessibility training programs across a global tech organization, enabling 12 distinct product engineering teams to independently audit, test with screen readers, and ship accessible code.")
    add_bullet(None, "End-to-End Program Delivery: ", "Managed end-to-end project timelines, provided deep technical remediation support to development teams, and collaborated across global cross-functional stakeholders.")
    add_bullet(None, "Cross-Platform Validation: ", "Conducted comprehensive accessibility audits across multiple operating systems (Android, ChromeOS, Desktop web) and AT combinations (TalkBack, ChromeVox, NVDA, JAWS).")
    add_bullet(None, "ACE Award 2023: ", "Honored with the ACE Award 2023—Tech Mahindra's highest individual performance recognition—for exceptional delivery and leadership on a global accessibility engagement.")

    # HCL Technologies
    add_subsection_heading("HCL Technologies", "Accessibility Technical Lead", "July 2014 – October 2019 | Chennai, India")
    
    p_hcl_client = doc.add_paragraph()
    p_hcl_client.paragraph_format.space_before = Pt(2)
    p_hcl_client.paragraph_format.space_after = Pt(2)
    p_hcl_client.paragraph_format.keep_with_next = True
    r = p_hcl_client.add_run("Client: Google India (Google Play)")
    r.font.bold = True
    r.font.color.rgb = SLATE

    add_bullet(None, "Sole End-User Accessibility Specialist: ", "Served as the dedicated primary accessibility tester and SME for the Google Play web and developer platform ecosystems.")
    add_bullet(None, "Audit Execution & Bug Tracking: ", "Executed thorough accessibility test suites against W3C WCAG standards and internal Google checklists, logging and verifying defects through enterprise issue-tracking workflows.")
    add_bullet(None, "Documentation & VPAT Standards: ", "Authored comprehensive accessibility audit reports, drafted formal compliance statements, and created reusable documentation standards for cross-team remediation.")

    # Prakat Solutions
    add_subsection_heading("Prakat Solutions", "Accessibility Tester & Consultant", "February 2011 – June 2014 | Bengaluru, India")
    
    p_prakat_client = doc.add_paragraph()
    p_prakat_client.paragraph_format.space_before = Pt(2)
    p_prakat_client.paragraph_format.space_after = Pt(2)
    p_prakat_client.paragraph_format.keep_with_next = True
    r = p_prakat_client.add_run("Client: Oracle Financial Services & Global Enterprise Clients")
    r.font.bold = True
    r.font.color.rgb = SLATE

    add_bullet(None, "Large-Scale Portfolio Auditing: ", "Evaluated accessibility across 250+ enterprise websites, internal web applications, PDF documents, and Flash solutions against Section 508 and WCAG standards.")
    add_bullet(None, "VPAT & Compliance Documentation: ", "Formulated detailed accessibility statements, assisted in VPAT generation, and delivered technical consulting on remediation strategies.")
    add_bullet(None, "Advocacy & Team Buy-In: ", "Advocated for inclusive design principles across client development teams, securing organizational buy-in for digital accessibility.")

    # Mphasis
    add_subsection_heading("Mphasis (an HP Company)", "Technical Support Engineer (L2)", "April 2009 – September 2010 | Pune, India")
    add_bullet(None, "Enterprise Support: ", "Handled complex Tier-2 customer queries covering HP hardware architecture, software diagnostics, and operating system troubleshooting.")

    # ----------------------------------------------------
    # 4. PRODUCT INNOVATION & OPEN-SOURCE LEADERSHIP
    # ----------------------------------------------------
    add_section_heading("Product Innovation & Open-Source Projects")

    add_subsection_heading("AMASAMYA — Open-Source Digital Accessibility Audit Suite", "Creator & Lead Architect", "2024 – Present")
    add_bullet(None, "Open-Source Innovation: ", "Independently conceived, architected, and shipped AMASAMYA (akhileshmalani.com/amasamya), an open-source accessibility auditing toolkit dedicated to making enterprise-grade accessibility testing free and accessible.")
    add_bullet(None, "Chrome Extension Architecture: ", "Engineered a browser extension featuring 24 automated WCAG 2.2 audit engines, multi-page site crawl capabilities (up to 200 pages per run), and a screen-reader/keyboard-first side-panel interface featuring ARIA live polite-region diff announcements.")
    add_bullet(None, "Free Online Accessibility Checker: ", "Developed and launched an interactive web-based HTML validator and compliance learning platform, enabling developers to obtain instant WCAG 2.2 AA issue categorization, severity rankings, and actionable fix guidance.")

    # ----------------------------------------------------
    # 5. HONORS, AWARDS & SPEAKING ENGAGEMENTS
    # ----------------------------------------------------
    add_section_heading("Honors, Awards & Thought Leadership")

    add_bullet(None, "ACE Award (2023) — Tech Mahindra: ", "Conferred the highest organizational performance award for stellar leadership, subject matter expertise, and delivery across a global accessibility engagement.")
    add_bullet(None, "Youngest Web Researcher (2011) — World Wide Web Conference (WWW2011): ", "Recognized at the prestigious international conference in Hyderabad for innovative assistive technology research (Android-based Indian currency recognition application for visually impaired users).")
    add_bullet(None, "Invited Speaker — NASSCOM BPO Summit (2010): ", "Presented on digital inclusion, accessibility in IT infrastructure, and career enablement for Persons with Disabilities.")
    add_bullet(None, "Keynote & Corporate Workshops: ", "Regularly delivers signature sessions including 'What Happens When a Blind User Tries Your Product', live screen-reader debugging workshops, and corporate seminars on WCAG 2.2, RPwD Act 2016, and SEBI accessibility compliance.")
    add_bullet(None, "Industry Publications & Articles: ", "Publishes technical guides and accessibility analyses on akhileshmalani.com/blog, covering banking accessibility breakdowns, ARIA best practices, and shift-left testing methodologies.")

    # ----------------------------------------------------
    # 6. EDUCATION
    # ----------------------------------------------------
    add_section_heading("Education & Academic Background")

    add_bullet(None, "Bachelor of Arts (B.A.) in English Literature", " | Loyola College, University of Madras, Chennai (2005 – 2008)")
    add_bullet(None, "Higher Senior Secondary Certificate (Arts)", " | Netraheen Vikas Sansthan, Jodhpur, Rajasthan (2003 – 2005)")
    add_bullet(None, "Senior Secondary Certificate (Arts)", " | Netraheen Vikas Sansthan, Jodhpur, Rajasthan (2001 – 2003)")

    # ----------------------------------------------------
    # 7. PERSONAL DETAILS
    # ----------------------------------------------------
    add_section_heading("Personal Details")

    add_bullet(None, "Languages Known: ", "English (Fluent), Hindi (Native), Tamil (Conversational), Marwari (Native)")
    add_bullet(None, "Interests & Community Involvement: ", "Music (Vocal & Multi-instrumentalist), Assistive Technology R&D, Mentoring & Career Guidance for visually impaired professionals")

    # Save document
    doc.save(output_path)
    print(f"Successfully created accessible CV at: {output_path}")

if __name__ == "__main__":
    out_dir = r"C:\Users\akhi_\antigravity\focused-fermi"
    out_file = os.path.join(out_dir, "Akhilesh_Malani_CV.docx")
    create_accessible_cv(out_file)
