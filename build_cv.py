# -*- coding: utf-8 -*-
"""
Builds the accessible, ATS-safe CV for Akhilesh Malani.

Design goals:
  * Real Word heading styles (outline levels 0-3) so NVDA / JAWS heading
    navigation works in both the .docx and the exported tagged PDF.
  * Real list numbering (no literal bullet characters typed as text).
  * Single-column linear reading order, no text boxes, no layout tables,
    no icons or graphics carrying meaning: safe for ATS text extraction.
  * Document title, author, and language set for PDF/UA metadata.
  * No em-dash or en-dash characters anywhere.
"""

import docx
from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn
from docx.shared import Pt, Inches, RGBColor

OUT = "Akhilesh_Malani_CV.docx"

INK = RGBColor(0x1F, 0x29, 0x37)      # body text
ACCENT = RGBColor(0x0F, 0x4C, 0x81)   # headings and rules
MUTED = RGBColor(0x4B, 0x55, 0x63)    # dates and locations
ACCENT_H = "0F4C81"

BODY_FONT = "Calibri"
BODY_SIZE = Pt(10.5)


# ---------------------------------------------------------------- helpers

def add_hyperlink(paragraph, url, text, size=BODY_SIZE):
    """Insert a real hyperlink run. Link text is the readable destination,
    never 'click here', so it makes sense when read out of context."""
    part = paragraph.part
    r_id = part.relate_to(
        url, docx.opc.constants.RELATIONSHIP_TYPE.HYPERLINK, is_external=True
    )
    link = parse_xml(
        '<w:hyperlink %s r:id="%s" '
        'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>'
        % (nsdecls("w"), r_id)
    )
    run = parse_xml("<w:r %s/>" % nsdecls("w"))
    rpr = parse_xml(
        '<w:rPr %s><w:rFonts w:ascii="%s" w:hAnsi="%s"/><w:color w:val="%s"/>'
        '<w:u w:val="single"/><w:sz w:val="%d"/></w:rPr>'
        % (nsdecls("w"), BODY_FONT, BODY_FONT, ACCENT_H, int(size.pt * 2))
    )
    run.append(rpr)
    t = parse_xml('<w:t %s xml:space="preserve"/>' % nsdecls("w"))
    t.text = text
    run.append(t)
    link.append(run)
    paragraph._p.append(link)


def rule_below(paragraph, size="8", color=ACCENT_H, space="6"):
    pPr = paragraph._p.get_or_add_pPr()
    pPr.append(parse_xml(
        '<w:pBdr %s><w:bottom w:val="single" w:sz="%s" w:space="%s" w:color="%s"/></w:pBdr>'
        % (nsdecls("w"), size, space, color)
    ))


def keep_with_next(paragraph):
    pPr = paragraph._p.get_or_add_pPr()
    pPr.append(parse_xml("<w:keepNext %s/>" % nsdecls("w")))
    pPr.append(parse_xml("<w:keepLines %s/>" % nsdecls("w")))


def styled_runs(paragraph, segments):
    """segments: list of (text, bold, italic, colour-or-None)."""
    for text, bold, italic, colour in segments:
        r = paragraph.add_run(text)
        r.font.name = BODY_FONT
        r.font.size = BODY_SIZE
        r.bold = bold
        r.italic = italic
        r.font.color.rgb = colour if colour else INK
    return paragraph


# ---------------------------------------------------------------- styles

def configure_styles(doc):
    normal = doc.styles["Normal"]
    normal.font.name = BODY_FONT
    normal.font.size = BODY_SIZE
    normal.font.color.rgb = INK
    normal.element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    pf = normal.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(4)
    pf.line_spacing = 1.12
    pf.widow_control = True

    def heading(name, size, space_before, space_after, colour, caps=False,
                bold=True):
        st = doc.styles[name]
        st.font.name = BODY_FONT
        st.font.size = Pt(size)
        st.font.bold = bold
        st.font.color.rgb = colour
        st.element.rPr.rFonts.set(qn("w:ascii"), BODY_FONT)
        st.element.rPr.rFonts.set(qn("w:hAnsi"), BODY_FONT)
        if caps:
            st.element.rPr.append(parse_xml("<w:caps %s/>" % nsdecls("w")))
            st.element.rPr.append(
                parse_xml('<w:spacing %s w:val="20"/>' % nsdecls("w"))
            )
        hpf = st.paragraph_format
        hpf.space_before = Pt(space_before)
        hpf.space_after = Pt(space_after)
        hpf.keep_with_next = True
        hpf.keep_together = True
        return st

    heading("Heading 1", 21, 0, 2, ACCENT)                # candidate name
    heading("Heading 2", 12, 11, 4, ACCENT, caps=True)    # section headings
    heading("Heading 3", 11.5, 8, 1, INK)                # employer + title
    heading("Heading 4", 10.5, 6, 2, ACCENT, bold=True)   # client engagement

    bullet = doc.styles["List Bullet"]
    bullet.font.name = BODY_FONT
    bullet.font.size = BODY_SIZE
    bullet.font.color.rgb = INK
    bpf = bullet.paragraph_format
    bpf.left_indent = Inches(0.25)
    bpf.first_line_indent = Inches(-0.19)
    bpf.space_before = Pt(0)
    bpf.space_after = Pt(2)
    bpf.line_spacing = 1.12


# ---------------------------------------------------------------- content

CONTACT_LOCATION = "Chennai, Tamil Nadu 602024, India"
PHONE = "+91 90005 31333"
EMAIL = "akhilesh.malani@gmail.com"

SUMMARY = [
    "Results-driven Lead Accessibility Architect and Subject Matter Expert with "
    "16+ years of specialized experience spearheading digital accessibility (a11y), "
    "inclusive UX architecture, and global regulatory compliance across enterprise "
    "web, native mobile (iOS and Android), and digital document platforms. Combines "
    "daily lived experience as a blind professional using screen readers with deep "
    "engineering acumen to identify critical barriers, focus traps, and complex "
    "interaction flaws that automated scanning tools miss.",

    "Proven track record leading high-impact accessibility initiatives for Tier-1 "
    "global organizations, including Bank of Montreal, Dun & Bradstreet, Google "
    "(Google Play and core products), and Oracle Financial Services. Creator of "
    "AMASAMYA, an open-source WCAG 2.2 audit toolkit and Chrome extension. Recipient "
    "of Tech Mahindra's ACE Award 2023 for outstanding delivery on a global "
    "accessibility program, and recognized at WWW2011 as Youngest Web Researcher.",
]

SKILLS = [
    ("Accessibility Standards and Frameworks",
     "WCAG 2.0 / 2.1 / 2.2 (Levels A, AA, AAA), WAI-ARIA 1.2 and 1.3 Authoring "
     "Practices, Section 508 (US Rehabilitation Act), Americans with Disabilities "
     "Act (ADA Title III), EN 301 549 (EU Accessibility Mandate), PDF/UA "
     "(ISO 14289), AODA, GIGW 3.0, RPwD Act 2016, SEBI Digital Accessibility "
     "Mandate."),
    ("Screen Readers and Assistive Technology",
     "JAWS, NVDA, Narrator (Windows), Apple VoiceOver (macOS, iOS and iPadOS), "
     "Google TalkBack (Android), Orca (Linux), ChromeVox (ChromeOS)."),
    ("Assistive Hardware and Input Methods",
     "Freedom Scientific Focus 40 Blue refreshable braille display, keyboard-only "
     "interaction testing."),
    ("Auditing, Strategy and Remediation Architecture",
     "Accessibility Conformance Reports (ACR based on VPAT 2.5), shift-left SDLC "
     "integration, technical remediation playbooks for front-end engineers, design "
     "system and component architecture covering complex data visualization, "
     "charts, dynamic modals and multi-step transaction flows, automated test "
     "script strategy and site crawl engines."),
    ("Platforms and Tooling",
     "Windows, macOS, Linux, iOS, Android, Microsoft Office suite, PDF "
     "accessibility tooling, browser developer tools and accessibility trees."),
    ("Leadership and Enablement",
     "Leading diverse testing teams including Persons with Disabilities, "
     "cross-functional stakeholder management, technical mentorship for engineering "
     "and UI/UX teams, Accessibility Champions programs, executive workshops."),
]

EXPERIENCE = [
    {
        "employer": "Virtusa Systems India Pvt. Ltd.",
        "title": "Accessibility Test Architect",
        "meta": "March 2024 to Present  |  Chennai, India",
        "clients": [
            {
                "name": "Client: Bank of Montreal (OLBB, Online Banking for Business)",
                "dates": "March 2025 to Present",
                "bullets": [
                    ("Lead Accessibility Architect",
                     "Govern end-to-end accessibility evaluation and architectural "
                     "remediation across BMO's customer-facing web applications, native "
                     "mobile apps (iOS and Android), and transactional digital "
                     "documents."),
                    ("Measurable Defect Reduction",
                     "Spearheaded a targeted remediation roadmap and developer "
                     "guidance framework that reduced critical accessibility defects "
                     "by 73% across two consecutive major release cycles."),
                    ("WCAG 2.2 and Document Compliance",
                     "Enforced strict compliance with WCAG 2.2 Level AA. Led "
                     "automated and manual validation for high-volume digital "
                     "statements and PDFs, ensuring full PDF/UA and WCAG alignment."),
                    ("VPAT 2.5 and Technical Playbooks",
                     "Standardized the creation and governance of Accessibility "
                     "Conformance Reports (ACR based on VPAT 2.5) and authored "
                     "cross-platform technical remediation playbooks for development "
                     "squads."),
                ],
            },
            {
                "name": "Client: Dun & Bradstreet",
                "dates": "April 2024 to February 2025",
                "bullets": [
                    ("Lead Accessibility Consultant",
                     "Acted as lead accessibility SME for the flagship Risk Analytics "
                     "web application, guaranteeing full compliance with WCAG 2.2 "
                     "Level AA."),
                    ("Complex UI and ARIA Implementation",
                     "Architected expert-level WAI-ARIA implementation strategies for "
                     "intricate data visualization components, interactive charting "
                     "widgets, and risk analytics dashboards."),
                    ("Shift-Left Champions Program",
                     "Established an Accessibility Champions program across design "
                     "and development squads, providing technical mentorship to UI/UX "
                     "designers and engineers to resolve defects early in the SDLC."),
                ],
            },
        ],
    },
    {
        "employer": "Tech Mahindra",
        "title": "Accessibility Test Lead and SME",
        "meta": "November 2019 to March 2024  |  Hyderabad and Chennai, India",
        "clients": [
            {
                "name": "Client: Google India",
                "dates": "",
                "bullets": [
                    ("Team Leadership",
                     "Led, managed, and mentored a high-performing accessibility "
                     "testing team of 15 engineers, including Persons with "
                     "Disabilities, executing Google's global accessibility standards."),
                    ("Engineering Enablement across 12 Teams",
                     "Designed and delivered hands-on accessibility training "
                     "programs that enabled 12 distinct product engineering teams "
                     "to independently audit, test with screen readers, and ship "
                     "accessible code."),
                    ("End-to-End Program Delivery",
                     "Managed project timelines, provided deep technical remediation "
                     "support to development teams, and collaborated across global "
                     "cross-functional stakeholders."),
                    ("Cross-Platform Validation",
                     "Conducted comprehensive audits across Android, ChromeOS, and "
                     "desktop web, using TalkBack, ChromeVox, NVDA, and JAWS."),
                    ("ACE Award 2023",
                     "Honored with the ACE Award 2023, Tech Mahindra's highest "
                     "individual performance recognition, for exceptional delivery "
                     "and leadership on a global accessibility engagement."),
                ],
            },
        ],
    },
    {
        "employer": "HCL Technologies",
        "title": "Accessibility Technical Lead",
        "meta": "July 2014 to October 2019  |  Chennai, India",
        "clients": [
            {
                "name": "Client: Google India (Google Play)",
                "dates": "",
                "bullets": [
                    ("Sole End-User Accessibility Specialist",
                     "Served as the dedicated primary accessibility tester and SME "
                     "for the Google Play web and developer platform ecosystems."),
                    ("Audit Execution and Defect Tracking",
                     "Executed thorough accessibility test suites against W3C WCAG "
                     "standards and internal Google checklists, logging and verifying "
                     "defects through enterprise issue-tracking workflows."),
                    ("Documentation and VPAT Standards",
                     "Authored comprehensive audit reports, drafted formal compliance "
                     "statements, and created reusable documentation standards for "
                     "cross-team remediation."),
                ],
            },
        ],
    },
    {
        "employer": "Prakat Solutions",
        "title": "Accessibility Tester and Consultant",
        "meta": "February 2011 to June 2014  |  Bengaluru, India",
        "clients": [
            {
                "name": "Client: Oracle Financial Services and global enterprise clients",
                "dates": "",
                "bullets": [
                    ("Large-Scale Portfolio Auditing",
                     "Evaluated accessibility across 250+ enterprise websites, "
                     "internal web applications, PDF documents, and Flash solutions "
                     "against Section 508 and WCAG standards."),
                    ("VPAT and Compliance Documentation",
                     "Formulated detailed accessibility statements, assisted in VPAT "
                     "generation, and delivered technical consulting on remediation "
                     "strategies."),
                    ("Advocacy and Team Buy-In",
                     "Advocated for inclusive design principles across client "
                     "development teams, securing organizational buy-in for digital "
                     "accessibility."),
                ],
            },
        ],
    },
    {
        "employer": "Mphasis (an HP Company)",
        "title": "Technical Support Engineer, Level 2",
        "meta": "April 2009 to September 2010  |  Pune, India",
        "clients": [
            {
                "name": "",
                "dates": "",
                "bullets": [
                    ("Enterprise Support",
                     "Handled complex Tier-2 customer queries covering HP hardware "
                     "architecture, software diagnostics, and operating system "
                     "troubleshooting."),
                ],
            },
        ],
    },
]

PROJECT_BULLETS = [
    ("Open-Source Innovation",
     "Independently conceived, architected, and shipped AMASAMYA "
     "(akhileshmalani.com/amasamya), an open-source accessibility auditing toolkit "
     "that makes enterprise-grade accessibility testing free to use."),
    ("Chrome Extension Architecture",
     "Engineered a browser extension with 24 automated WCAG 2.2 audit engines, "
     "multi-page site crawl of up to 200 pages per run, and a screen-reader and "
     "keyboard-first side-panel interface with ARIA live polite-region diff "
     "announcements."),
    ("Free Online Accessibility Checker",
     "Built and launched a web-based HTML validator and compliance learning "
     "platform giving developers instant WCAG 2.2 AA issue categorization, "
     "severity ranking, and actionable fix guidance."),
]

HONOURS = [
    ("ACE Award, 2023, Tech Mahindra",
     "Highest organizational performance award, conferred for leadership, subject "
     "matter expertise, and delivery across a global accessibility engagement."),
    ("Youngest Web Researcher, 2011, World Wide Web Conference (WWW2011)",
     "Recognized at the international conference in Hyderabad for assistive "
     "technology research: an Android-based Indian currency recognition application "
     "for visually impaired users."),
    ("Invited Speaker, NASSCOM BPO Summit, 2010",
     "Presented on digital inclusion, accessibility in IT infrastructure, and "
     "career enablement for Persons with Disabilities."),
    ("Keynotes and Corporate Workshops",
     "Delivers signature sessions including 'What Happens When a Blind User Tries "
     "Your Product', live screen-reader debugging workshops, and corporate seminars "
     "on WCAG 2.2, the RPwD Act 2016, and SEBI accessibility compliance."),
    ("Industry Publications",
     "Publishes technical guides and accessibility analyses at "
     "akhileshmalani.com/blog, covering banking accessibility breakdowns, ARIA best "
     "practices, and shift-left testing methodology."),
]

EDUCATION = [
    ("Bachelor of Arts (B.A.), English Literature",
     "Loyola College, University of Madras, Chennai  |  2005 to 2008"),
    ("Higher Senior Secondary Certificate (Arts)",
     "Netraheen Vikas Sansthan, Jodhpur, Rajasthan  |  2003 to 2005"),
    ("Senior Secondary Certificate (Arts)",
     "Netraheen Vikas Sansthan, Jodhpur, Rajasthan  |  2001 to 2003"),
]

PERSONAL = [
    ("Languages",
     "English (fluent), Hindi (native), Marwari (native), Tamil (conversational)."),
    ("Interests and Community Involvement",
     "Music as a vocalist and multi-instrumentalist, assistive technology research "
     "and development, mentoring and career guidance for visually impaired "
     "professionals."),
]


# ---------------------------------------------------------------- builder

def build():
    doc = Document()

    props = doc.core_properties
    props.title = "Akhilesh Malani, Curriculum Vitae"
    props.author = "Akhilesh Malani"
    props.subject = "Lead Accessibility Test Architect and Digital Inclusion SME"
    props.keywords = ("Accessibility Architect, WCAG 2.2, WAI-ARIA, Section 508, "
                      "EN 301 549, PDF/UA, VPAT, Screen Readers, NVDA, JAWS, "
                      "Digital Inclusion")
    props.category = "Curriculum Vitae"
    props.comments = ("Accessible CV structured with real heading levels and "
                      "list semantics.")

    section = doc.sections[0]
    section.top_margin = Inches(0.6)
    section.bottom_margin = Inches(0.6)
    section.left_margin = Inches(0.75)
    section.right_margin = Inches(0.75)

    configure_styles(doc)

    # -- masthead ---------------------------------------------------------
    name = doc.add_paragraph("AKHILESH MALANI", style="Heading 1")
    name.alignment = WD_ALIGN_PARAGRAPH.CENTER

    tag = doc.add_paragraph()
    tag.alignment = WD_ALIGN_PARAGRAPH.CENTER
    tag.paragraph_format.space_after = Pt(4)
    styled_runs(tag, [(
        "Lead Accessibility Test Architect  |  Digital Inclusion and "
        "Assistive Technology SME", True, False, MUTED)])

    line1 = doc.add_paragraph()
    line1.alignment = WD_ALIGN_PARAGRAPH.CENTER
    line1.paragraph_format.space_after = Pt(2)
    styled_runs(line1, [(CONTACT_LOCATION + "  |  " + PHONE + "  |  ",
                         False, False, MUTED)])
    add_hyperlink(line1, "mailto:" + EMAIL, EMAIL)

    line2 = doc.add_paragraph()
    line2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    line2.paragraph_format.space_after = Pt(2)
    add_hyperlink(line2, "https://akhileshmalani.com", "akhileshmalani.com")
    styled_runs(line2, [("  |  ", False, False, MUTED)])
    add_hyperlink(line2, "https://www.linkedin.com/in/akhilesh-malani",
                  "linkedin.com/in/akhilesh-malani")
    styled_runs(line2, [("  |  ", False, False, MUTED)])
    add_hyperlink(line2, "https://akhileshmalani.com/amasamya",
                  "akhileshmalani.com/amasamya")
    rule_below(line2, size="10", space="8")

    def h2(text):
        return doc.add_paragraph(text, style="Heading 2")

    def lead_bullet(label, body):
        p = doc.add_paragraph(style="List Bullet")
        styled_runs(p, [(label + ": ", True, False, INK),
                        (body, False, False, INK)])
        return p

    # -- summary ----------------------------------------------------------
    h2("Professional Summary")
    for para in SUMMARY:
        p = doc.add_paragraph()
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        p.paragraph_format.space_after = Pt(5)
        styled_runs(p, [(para, False, False, INK)])

    # -- skills -----------------------------------------------------------
    h2("Core Competencies and Technical Skills")
    for label, body in SKILLS:
        lead_bullet(label, body)

    # -- experience -------------------------------------------------------
    h2("Professional Experience")
    for job in EXPERIENCE:
        h = doc.add_paragraph(style="Heading 3")
        r0 = h.add_run(job["employer"])
        r0.font.name = BODY_FONT
        r0.font.size = Pt(11.5)
        r0.bold = True
        r0.font.color.rgb = INK
        r1 = h.add_run("  |  " + job["title"])
        r1.font.name = BODY_FONT
        r1.font.size = Pt(11.5)
        r1.bold = False
        r1.font.color.rgb = ACCENT

        meta = doc.add_paragraph()
        meta.paragraph_format.space_after = Pt(3)
        keep_with_next(meta)
        styled_runs(meta, [(job["meta"], False, True, MUTED)])

        for client in job["clients"]:
            if client["name"]:
                ch = doc.add_paragraph(style="Heading 4")
                label = client["name"]
                if client["dates"]:
                    label += "  |  " + client["dates"]
                styled_runs(ch, [(label, True, False, ACCENT)])
            for lab, body in client["bullets"]:
                lead_bullet(lab, body)

    # -- projects ---------------------------------------------------------
    h2("Product Innovation and Open-Source Projects")
    h = doc.add_paragraph(style="Heading 3")
    r0 = h.add_run("AMASAMYA, Open-Source Digital Accessibility Audit Suite")
    r0.font.name = BODY_FONT
    r0.font.size = Pt(11.5)
    r0.bold = True
    r0.font.color.rgb = INK
    r1 = h.add_run("  |  Creator and Lead Architect")
    r1.font.name = BODY_FONT
    r1.font.size = Pt(11.5)
    r1.bold = False
    r1.font.color.rgb = ACCENT

    meta = doc.add_paragraph()
    meta.paragraph_format.space_after = Pt(3)
    keep_with_next(meta)
    styled_runs(meta, [("2024 to Present", False, True, MUTED)])
    for lab, body in PROJECT_BULLETS:
        lead_bullet(lab, body)

    # -- honours ----------------------------------------------------------
    h2("Honors, Awards and Thought Leadership")
    for lab, body in HONOURS:
        lead_bullet(lab, body)

    # -- education --------------------------------------------------------
    h2("Education")
    for lab, body in EDUCATION:
        lead_bullet(lab, body)

    # -- personal ---------------------------------------------------------
    h2("Personal Details")
    for lab, body in PERSONAL:
        lead_bullet(lab, body)

    # -- footer: name and page number, no contact data (ATS safe) ---------
    fp = doc.sections[0].footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    fr = fp.add_run("Akhilesh Malani  |  Curriculum Vitae  |  Page ")
    fr.font.name = BODY_FONT
    fr.font.size = Pt(8.5)
    fr.font.color.rgb = MUTED
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), " PAGE ")
    inner_r = OxmlElement("w:r")
    inner_r.append(parse_xml(
        '<w:rPr %s><w:rFonts w:ascii="%s" w:hAnsi="%s"/><w:color w:val="4B5563"/>'
        '<w:sz w:val="17"/></w:rPr>' % (nsdecls("w"), BODY_FONT, BODY_FONT)))
    inner_t = parse_xml('<w:t %s/>' % nsdecls("w"))
    inner_t.text = "1"
    inner_r.append(inner_t)
    fld.append(inner_r)
    fp._p.append(fld)

    doc.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    build()
