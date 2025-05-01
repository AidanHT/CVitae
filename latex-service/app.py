#!/usr/bin/env python3
"""
CVitae LaTeX Service
Microservice for compiling LaTeX resumes to PDF and image formats
"""

import os
import subprocess
import tempfile
import shutil
from pathlib import Path
from flask import Flask, request, jsonify, send_file
from werkzeug.utils import secure_filename
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024  # 16MB max file size

class LaTeXCompiler:
    """LaTeX compilation service"""
    
    def __init__(self):
        self.temp_dir = Path(tempfile.gettempdir()) / "cvitae_latex"
        self.temp_dir.mkdir(exist_ok=True)
        
    def compile_latex_to_pdf(self, latex_content: str, output_name: str = "resume") -> str:
        """Compile LaTeX content to PDF"""
        try:
            # Create temporary directory for this compilation
            work_dir = self.temp_dir / f"{output_name}_{os.getpid()}"
            work_dir.mkdir(exist_ok=True)
            
            # Write LaTeX content to file
            tex_file = work_dir / f"{output_name}.tex"
            with open(tex_file, 'w', encoding='utf-8') as f:
                f.write(latex_content)
            
            # Compile LaTeX to PDF
            cmd = [
                'pdflatex', 
                '-interaction=nonstopmode',
                '-output-directory', str(work_dir),
                str(tex_file)
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True, cwd=work_dir)
            
            if result.returncode != 0:
                logger.error(f"LaTeX compilation failed: {result.stderr}")
                raise Exception(f"LaTeX compilation failed: {result.stderr}")
            
            pdf_file = work_dir / f"{output_name}.pdf"
            
            if not pdf_file.exists():
                raise Exception("PDF file was not generated")
            
            return str(pdf_file)
            
        except Exception as e:
            logger.error(f"Error compiling LaTeX: {str(e)}")
            raise
    
    def pdf_to_image(self, pdf_path: str, output_format: str = "png", dpi: int = 300) -> str:
        """Convert PDF to image format"""
        try:
            work_dir = Path(pdf_path).parent
            output_file = work_dir / f"resume.{output_format}"
            
            # Use ImageMagick to convert PDF to image
            cmd = [
                'convert',
                '-density', str(dpi),
                '-quality', '100',
                pdf_path,
                str(output_file)
            ]
            
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            if result.returncode != 0:
                logger.error(f"Image conversion failed: {result.stderr}")
                raise Exception(f"Image conversion failed: {result.stderr}")
            
            if not output_file.exists():
                raise Exception("Image file was not generated")
            
            return str(output_file)
            
        except Exception as e:
            logger.error(f"Error converting PDF to image: {str(e)}")
            raise
    
    def cleanup(self, work_dir: str):
        """Clean up temporary files"""
        try:
            shutil.rmtree(work_dir)
        except Exception as e:
            logger.warning(f"Failed to cleanup {work_dir}: {str(e)}")

# Initialize compiler
compiler = LaTeXCompiler()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "service": "cvitae-latex"})

@app.route('/compile/pdf', methods=['POST'])
def compile_to_pdf():
    """Compile LaTeX to PDF"""
    try:
        data = request.get_json()
        
        if not data or 'latex' not in data:
            return jsonify({"error": "LaTeX content is required"}), 400
        
        latex_content = data['latex']
        output_name = data.get('name', 'resume')
        
        # Compile LaTeX to PDF
        pdf_path = compiler.compile_latex_to_pdf(latex_content, output_name)
        
        # Return PDF file
        return send_file(
            pdf_path,
            as_attachment=True,
            download_name=f"{output_name}.pdf",
            mimetype='application/pdf'
        )
        
    except Exception as e:
        logger.error(f"Error in compile_to_pdf: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/compile/image', methods=['POST'])
def compile_to_image():
    """Compile LaTeX to image format"""
    try:
        data = request.get_json()
        
        if not data or 'latex' not in data:
            return jsonify({"error": "LaTeX content is required"}), 400
        
        latex_content = data['latex']
        output_name = data.get('name', 'resume')
        format_type = data.get('format', 'png').lower()
        dpi = data.get('dpi', 300)
        
        if format_type not in ['png', 'jpg', 'jpeg']:
            return jsonify({"error": "Unsupported image format"}), 400
        
        # Compile LaTeX to PDF first
        pdf_path = compiler.compile_latex_to_pdf(latex_content, output_name)
        
        # Convert PDF to image
        image_path = compiler.pdf_to_image(pdf_path, format_type, dpi)
        
        # Determine MIME type
        mime_type = {
            'png': 'image/png',
            'jpg': 'image/jpeg',
            'jpeg': 'image/jpeg'
        }.get(format_type, 'image/png')
        
        # Return image file
        return send_file(
            image_path,
            as_attachment=True,
            download_name=f"{output_name}.{format_type}",
            mimetype=mime_type
        )
        
    except Exception as e:
        logger.error(f"Error in compile_to_image: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/validate', methods=['POST'])
def validate_latex():
    """Validate LaTeX syntax without compilation"""
    try:
        data = request.get_json()
        
        if not data or 'latex' not in data:
            return jsonify({"error": "LaTeX content is required"}), 400
        
        latex_content = data['latex']
        
        # Basic validation checks
        validation_errors = []
        
        # Check for basic LaTeX structure
        if '\\documentclass' not in latex_content:
            validation_errors.append("Missing \\documentclass")
        
        if '\\begin{document}' not in latex_content:
            validation_errors.append("Missing \\begin{document}")
        
        if '\\end{document}' not in latex_content:
            validation_errors.append("Missing \\end{document}")
        
        # Check for balanced braces (simplified)
        open_braces = latex_content.count('{')
        close_braces = latex_content.count('}')
        if open_braces != close_braces:
            validation_errors.append("Unbalanced braces")
        
        return jsonify({
            "valid": len(validation_errors) == 0,
            "errors": validation_errors
        })
        
    except Exception as e:
        logger.error(f"Error in validate_latex: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/templates/jakes', methods=['GET'])
def get_jakes_template():
    """Get Jake's LaTeX resume template"""
    jakes_template = """
\\documentclass[letterpaper,11pt]{article}

\\usepackage{latexsym}
\\usepackage[empty]{fullpage}
\\usepackage{titlesec}
\\usepackage{marvosym}
\\usepackage[usenames,dvipsnames]{color}
\\usepackage{verbatim}
\\usepackage{enumitem}
\\usepackage[hidelinks]{hyperref}
\\usepackage{fancyhdr}
\\usepackage[english]{babel}
\\usepackage{tabularx}
\\input{glyphtounicode}

% Font options
\\usepackage[sfdefault]{FiraSans}
\\usepackage[T1]{fontenc}

\\pagestyle{fancy}
\\fancyhf{} 
\\fancyfoot{}
\\renewcommand{\\headrulewidth}{0pt}
\\renewcommand{\\footrulewidth}{0pt}

% Adjust margins
\\addtolength{\\oddsidemargin}{-0.5in}
\\addtolength{\\evensidemargin}{-0.5in}
\\addtolength{\\textwidth}{1in}
\\addtolength{\\topmargin}{-.5in}
\\addtolength{\\textheight}{1.0in}

\\urlstyle{same}

\\raggedbottom
\\raggedright
\\setlength{\\tabcolsep}{0in}

% Sections formatting
\\titleformat{\\section}{
  \\vspace{-4pt}\\scshape\\raggedright\\large
}{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]

% Ensure that generate pdf is machine readable/ATS parsable
\\pdfgentounicode=1

% Custom commands
\\newcommand{\\resumeItem}[1]{
  \\item\\small{
    {#1 \\vspace{-2pt}}
  }
}

\\newcommand{\\resumeSubheading}[4]{
  \\vspace{-2pt}\\item
    \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
      \\textbf{#1} & #2 \\\\
      \\textit{\\small#3} & \\textit{\\small #4} \\\\
    \\end{tabular*}\\vspace{-7pt}
}

\\newcommand{\\resumeSubSubheading}[2]{
    \\item
    \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
      \\textit{\\small#1} & \\textit{\\small #2} \\\\
    \\end{tabular*}\\vspace{-7pt}
}

\\newcommand{\\resumeProjectHeading}[2]{
    \\item
    \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
      \\small#1 & #2 \\\\
    \\end{tabular*}\\vspace{-7pt}
}

\\newcommand{\\resumeSubItem}[1]{\\resumeItem{#1}\\vspace{-4pt}}

\\renewcommand\\labelitemii{$\\vcenter{\\hbox{\\tiny$\\bullet$}}$}

\\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
\\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
\\newcommand{\\resumeItemListStart}{\\begin{itemize}}
\\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}

\\begin{document}

% HEADING
\\begin{center}
    \\textbf{\\Huge \\scshape [Your Name]} \\\\ \\vspace{1pt}
    \\small [Phone] $|$ \\href{mailto:[email]}{\\underline{[email]}} $|$ 
    \\href{[linkedin]}{\\underline{linkedin.com/in/[username]}} $|$
    \\href{[github]}{\\underline{github.com/[username]}}
\\end{center}

% EDUCATION
\\section{Education}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {[University Name]}{[Location]}
      {[Degree] in [Major]}{[Start Date] -- [End Date]}
  \\resumeSubHeadingListEnd

% EXPERIENCE
\\section{Experience}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {[Job Title]}{[Start Date] -- [End Date]}
      {[Company Name]}{[Location]}
      \\resumeItemListStart
        \\resumeItem{[Achievement or responsibility]}
        \\resumeItem{[Achievement or responsibility]}
      \\resumeItemListEnd
  \\resumeSubHeadingListEnd

% PROJECTS
\\section{Projects}
    \\resumeSubHeadingListStart
      \\resumeProjectHeading
          {\\textbf{[Project Name]} $|$ \\emph{[Technologies Used]}}{[Date]}
          \\resumeItemListStart
            \\resumeItem{[Project description or achievement]}
          \\resumeItemListEnd
    \\resumeSubHeadingListEnd

% TECHNICAL SKILLS
\\section{Technical Skills}
 \\begin{itemize}[leftmargin=0.15in, label={}]
    \\small{\\item{
     \\textbf{Languages}{: [Programming Languages]} \\\\
     \\textbf{Frameworks}{: [Frameworks and Libraries]} \\\\
     \\textbf{Developer Tools}{: [Tools and Software]} \\\\
     \\textbf{Libraries}{: [Additional Libraries]}
    }}
 \\end{itemize}

\\end{document}
"""
    
    return jsonify({
        "template": jakes_template.strip(),
        "name": "Jake's Resume Template",
        "description": "Clean, ATS-friendly resume template"
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)
