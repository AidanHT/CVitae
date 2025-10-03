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
logging.basicConfig(
    level=logging.DEBUG,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
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
        work_dir = None
        try:
            logger.info(f"Starting LaTeX compilation for {output_name}")
            
            # Pre-validate LaTeX content
            if not latex_content or not latex_content.strip():
                raise Exception("Empty LaTeX content provided")
            
            # Basic LaTeX validation
            if "\\documentclass" not in latex_content:
                raise Exception("LaTeX content missing \\documentclass declaration")
            
            if "\\begin{document}" not in latex_content:
                raise Exception("LaTeX content missing \\begin{document}")
            
            if "\\end{document}" not in latex_content:
                logger.warning("LaTeX content missing \\end{document}, attempting to add")
                latex_content = latex_content.rstrip() + "\n\\end{document}"
            
            # Create temporary directory for this compilation
            work_dir = self.temp_dir / f"{output_name}_{os.getpid()}"
            work_dir.mkdir(exist_ok=True)
            logger.debug(f"Created work directory: {work_dir}")
            
            # Write LaTeX content to file
            tex_file = work_dir / f"{output_name}.tex"
            with open(tex_file, 'w', encoding='utf-8') as f:
                f.write(latex_content)
            logger.debug(f"Written LaTeX content to: {tex_file}")
            logger.debug(f"LaTeX content preview: {latex_content[:300]}...")
            
            # First attempt: Use latexmk for compilation
            cmd = [
                'latexmk',
                '-pdf',
                '-interaction=nonstopmode',
                '-halt-on-error',
                '-file-line-error',
                '-output-directory=' + str(work_dir),
                str(tex_file)
            ]
            
            logger.debug(f"Running command: {' '.join(cmd)}")
            result = subprocess.run(cmd, capture_output=True, text=True, cwd=work_dir)
            
            logger.debug(f"latexmk return code: {result.returncode}")
            if result.stdout:
                logger.debug(f"latexmk stdout: {result.stdout}")
            if result.stderr:
                logger.debug(f"latexmk stderr: {result.stderr}")
            
            if result.returncode != 0:
                # Try alternative compilation method
                logger.warning("latexmk failed, trying direct pdflatex compilation")
                result = self.try_direct_pdflatex(tex_file, work_dir)
            
            if result.returncode != 0:
                # Get detailed error information
                log_file = work_dir / f"{output_name}.log"
                log_content = ""
                if log_file.exists():
                    try:
                        with open(log_file, 'r', encoding='utf-8', errors='ignore') as f:
                            log_content = f.read()
                        logger.error(f"LaTeX log file content: {log_content}")
                    except Exception:
                        pass
                
                # Parse and format the error for better debugging
                parsed_errors = self.parse_latex_log(log_content) if log_content else {}
                
                error_msg = f"LaTeX compilation failed (return code: {result.returncode})\nSTDERR: {result.stderr}\nSTDOUT: {result.stdout}"
                if log_content:
                    error_msg += f"\nLOG: {log_content[-2000:]}"  # Last 2000 chars
                
                # Add parsed error analysis
                if parsed_errors:
                    error_msg += f"\n\nERROR ANALYSIS:\n"
                    for error_type, errors in parsed_errors.items():
                        if errors:
                            error_msg += f"{error_type.upper()}: {', '.join(errors)}\n"
                
                logger.error(error_msg)
                raise Exception(error_msg)
            
            pdf_file = work_dir / f"{output_name}.pdf"
            
            if not pdf_file.exists():
                logger.error(f"PDF file was not generated at: {pdf_file}")
                # List files in work directory for debugging
                files = list(work_dir.glob("*"))
                logger.error(f"Files in work directory: {files}")
                raise Exception("PDF file was not generated")
            
            logger.info(f"Successfully compiled LaTeX to PDF: {pdf_file}")
            return str(pdf_file)
            
        except Exception as e:
            logger.error(f"Error compiling LaTeX: {str(e)}")
            # Don't cleanup on error so we can debug
            raise

    def pdf_to_image(self, pdf_path: str, output_format: str = "png", dpi: int = 300) -> str:
        """Convert PDF to image format"""
        try:
            logger.info(f"Converting PDF to {output_format} at {dpi} DPI")
            work_dir = Path(pdf_path).parent
            output_file = work_dir / f"resume.{output_format}"
            
            # Use ImageMagick to convert PDF to image
            cmd = [
                'convert',
                '-density', str(dpi),
                '-quality', '100',
                '-background', 'white',
                '-alpha', 'remove',
                pdf_path,
                str(output_file)
            ]
            
            logger.debug(f"Running ImageMagick command: {' '.join(cmd)}")
            result = subprocess.run(cmd, capture_output=True, text=True)
            
            logger.debug(f"ImageMagick return code: {result.returncode}")
            if result.stdout:
                logger.debug(f"ImageMagick stdout: {result.stdout}")
            if result.stderr:
                logger.debug(f"ImageMagick stderr: {result.stderr}")
            
            if result.returncode != 0:
                error_msg = f"Image conversion failed (return code: {result.returncode})\nSTDERR: {result.stderr}\nSTDOUT: {result.stdout}"
                logger.error(error_msg)
                raise Exception(error_msg)
            
            if not output_file.exists():
                logger.error(f"Image file was not generated at: {output_file}")
                # List files in work directory for debugging
                files = list(work_dir.glob("*"))
                logger.error(f"Files in work directory: {files}")
                raise Exception("Image file was not generated")
            
            logger.info(f"Successfully converted PDF to image: {output_file}")
            return str(output_file)
            
        except Exception as e:
            logger.error(f"Error converting PDF to image: {str(e)}")
            raise
    
    def parse_latex_log(self, log_content: str) -> dict:
        """Parse LaTeX log to extract actionable error information"""
        import re
        
        errors = {
            'missing_document': [],
            'undefined_commands': [],
            'lonely_items': [],
            'misaligned_amp': [],
            'missing_dollar': [],
            'font_errors': [],
            'package_errors': []
        }
        
        if not log_content:
            return errors
        
        # Check for missing \begin{document}
        if re.search(r'Missing \\begin\{document\}', log_content):
            errors['missing_document'].append('Document missing \\begin{document}')
        
        # Extract undefined control sequences
        undefined_matches = re.findall(r'Undefined control sequence\.\s*l\.\d+\s+(\\[A-Za-z@]+)', log_content)
        errors['undefined_commands'] = list(set(undefined_matches))  # Remove duplicates
        
        # Check for lonely items
        if re.search(r'Lonely \\item', log_content):
            errors['lonely_items'].append('\\item commands outside list environments')
        
        # Check for misaligned ampersands
        if re.search(r'Misplaced alignment tab character &', log_content):
            errors['misaligned_amp'].append('Unescaped & characters outside tables')
        
        # Check for missing dollar signs
        if re.search(r'Missing \$ inserted', log_content):
            errors['missing_dollar'].append('Unescaped $ characters or math mode issues')
        
        # Check for font errors
        if re.search(r'Font .* not found', log_content):
            errors['font_errors'].append('Missing font files')
        
        # Check for package errors
        package_error_matches = re.findall(r'! LaTeX Error: File `([^\']+)\' not found', log_content)
        if package_error_matches:
            errors['package_errors'] = list(set(package_error_matches))
        
        return {k: v for k, v in errors.items() if v}  # Return only non-empty error categories

    def cleanup(self, work_dir: str):
        """Clean up temporary files"""
        try:
            shutil.rmtree(work_dir)
        except Exception as e:
            logger.warning(f"Failed to cleanup {work_dir}: {str(e)}")

    def try_direct_pdflatex(self, tex_file: Path, work_dir: Path) -> subprocess.CompletedProcess:
        """Try direct pdflatex compilation as fallback"""
        try:
            # Run pdflatex multiple times to resolve references
            for i in range(3):
                cmd = [
                    'pdflatex',
                    '-interaction=nonstopmode',
                    '-halt-on-error',
                    '-file-line-error',
                    '-output-directory=' + str(work_dir),
                    str(tex_file)
                ]
                
                result = subprocess.run(cmd, capture_output=True, text=True, cwd=work_dir)
                
                if result.returncode == 0:
                    logger.info(f"Direct pdflatex succeeded on attempt {i+1}")
                    return result
                
                if i == 0:  # Only log first attempt details
                    logger.debug(f"pdflatex attempt {i+1} failed with code {result.returncode}")
            
            return result
            
        except Exception as e:
            logger.error(f"Direct pdflatex compilation failed: {e}")
            # Return a failed result
            return subprocess.CompletedProcess([], 1, "", str(e))

# Initialize compiler
compiler = LaTeXCompiler()

def check_system_dependencies():
    """Check that all required system dependencies are available"""
    dependencies = ['latexmk', 'pdflatex', 'convert']
    missing = []
    
    for dep in dependencies:
        try:
            # Different version flags for different tools
            version_flag = '--version' if dep != 'latexmk' else '-version'
            result = subprocess.run([dep, version_flag], capture_output=True, text=True)
            if result.returncode == 0:
                logger.info(f"✅ {dep} is available")
            else:
                missing.append(dep)
        except FileNotFoundError:
            missing.append(dep)
    
    if missing:
        logger.error(f"❌ Missing required dependencies: {missing}")
        raise RuntimeError(f"Missing system dependencies: {missing}")
    
    logger.info("✅ All system dependencies are available")

# Check dependencies on startup
check_system_dependencies()

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    return jsonify({"status": "healthy", "service": "cvitae-latex"})

@app.route('/compile/pdf', methods=['POST'])
def compile_to_pdf():
    """Compile LaTeX to PDF"""
    try:
        logger.info("Received PDF compilation request")
        data = request.get_json()
        
        if not data or 'latex' not in data:
            logger.error("Missing LaTeX content in request")
            return jsonify({"error": "LaTeX content is required"}), 400
        
        latex_content = data['latex']
        output_name = data.get('name', 'resume')
        
        logger.info(f"Compiling LaTeX to PDF: {output_name}")
        logger.debug(f"LaTeX content length: {len(latex_content)} characters")
        
        # Compile LaTeX to PDF
        pdf_path = compiler.compile_latex_to_pdf(latex_content, output_name)
        
        logger.info(f"Successfully compiled PDF, returning file: {pdf_path}")
        
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
        logger.info("Received image compilation request")
        data = request.get_json()
        
        if not data or 'latex' not in data:
            logger.error("Missing LaTeX content in request")
            return jsonify({"error": "LaTeX content is required"}), 400
        
        latex_content = data['latex']
        output_name = data.get('name', 'resume')
        format_type = data.get('format', 'png').lower()
        dpi = data.get('dpi', 300)
        
        logger.info(f"Compiling LaTeX to {format_type} image: {output_name} at {dpi} DPI")
        logger.debug(f"LaTeX content length: {len(latex_content)} characters")
        
        if format_type not in ['png', 'jpg', 'jpeg']:
            logger.error(f"Unsupported image format: {format_type}")
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
        
        logger.info(f"Successfully compiled image, returning file: {image_path}")
        
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

%% Core packages for professional resume (ATS-friendly)
\\usepackage{latexsym}
\\usepackage[empty]{fullpage}
\\usepackage{titlesec}
\\usepackage{marvosym}
\\usepackage[usenames,dvipsnames]{color}
\\usepackage{enumitem}
\\usepackage[hidelinks]{hyperref}
\\usepackage{fancyhdr}
\\usepackage[english]{babel}
\\usepackage{tabularx}
\\usepackage[utf8]{inputenc}
\\usepackage[T1]{fontenc}
\\usepackage{lmodern}
\\input{glyphtounicode}

%% Page formatting and margins
\\pagestyle{fancy}
\\fancyhf{}
\\fancyfoot{}
\\renewcommand{\\headrulewidth}{0pt}
\\renewcommand{\\footrulewidth}{0pt}
\\setlength{\\headheight}{14pt}  % Fix fancyhdr warning
\\addtolength{\\oddsidemargin}{-0.5in}
\\addtolength{\\evensidemargin}{-0.5in}
\\addtolength{\\textwidth}{1in}
\\addtolength{\\topmargin}{-.5in}
\\addtolength{\\textheight}{1.0in}
\\urlstyle{same}
\\raggedbottom
\\raggedright
\\setlength{\\tabcolsep}{0in}
\\setlength{\\parindent}{0pt}

%% Section title formatting
\\titleformat{\\section}{
  \\vspace{-4pt}\\scshape\\raggedright\\large
}{}{0em}{}[\\color{black}\\titlerule \\vspace{-5pt}]

%% PDF settings for ATS compatibility
\\pdfgentounicode=1

%% ========== JAKE'S RESUME TEMPLATE MACROS ==========
%% These macros define the structure and formatting for resume elements

%% Basic item with proper spacing
\\newcommand{\\resumeItem}[1]{
  \\item\\small{
    {#1 \\vspace{-2pt}}
  }
}

%% Four-argument subheading for positions/education
\\newcommand{\\resumeSubheading}[4]{
  \\vspace{-2pt}\\item
    \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
      \\textbf{#1} & #2 \\\\
      \\textit{\\small#3} & \\textit{\\small #4} \\\\
    \\end{tabular*}\\vspace{-7pt}
}

%% Education-specific subheading with proper formatting
\\newcommand{\\resumeSubheadingEducation}[4]{
  \\vspace{-2pt}\\item
    \\begin{tabular*}{0.97\\textwidth}[t]{l@{\\extracolsep{\\fill}}r}
      \\textbf{#1} & #2 \\\\
      \\textit{\\small#3} & \\textit{\\small #4} \\\\
    \\end{tabular*}\\vspace{-7pt}
}

%% Two-argument project heading
\\newcommand{\\resumeProjectHeading}[2]{
  \\item
    \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
      \\small#1 & #2 \\\\
    \\end{tabular*}\\vspace{-7pt}
}

%% Alternative subheading styles for flexibility
\\newcommand{\\resumeSubSubheading}[2]{
  \\item
    \\begin{tabular*}{0.97\\textwidth}{l@{\\extracolsep{\\fill}}r}
      \\textit{\\small#1} & \\textit{\\small #2} \\\\
    \\end{tabular*}\\vspace{-7pt}
}

%% List environment commands
\\newcommand{\\resumeSubHeadingListStart}{\\begin{itemize}[leftmargin=0.15in, label={}]}
\\newcommand{\\resumeSubHeadingListEnd}{\\end{itemize}}
\\newcommand{\\resumeItemListStart}{\\begin{itemize}}
\\newcommand{\\resumeItemListEnd}{\\end{itemize}\\vspace{-5pt}}

%% Custom bullet for nested lists
\\renewcommand\\labelitemii{$\\vcenter{\\hbox{\\tiny$\\bullet$}}$}

%% Skills section helper
\\newcommand{\\resumeSkillItem}[2]{
  \\item{\\textbf{#1:} #2}
}

%% Additional macros for common resume elements
\\newcommand{\\resumeAward}[2]{
  \\item \\textbf{#1} \\hfill #2
}

\\newcommand{\\resumeCertification}[2]{
  \\item #1 \\hfill \\textit{#2}
}

%% Legacy macro for compatibility
\\newcommand{\\resumeSubItem}[1]{\\resumeItem{#1}\\vspace{-4pt}}

%% Safe text escaping helpers (backup macros)
\\newcommand{\\safeampersand}{\\&}
\\newcommand{\\safedollar}{\\$}
\\newcommand{\\safepercent}{\\%}
\\newcommand{\\safeunderscore}{\\_}

\\begin{document}

%% HEADER SECTION
\\begin{center}
    \\textbf{\\Huge \\scshape [Your Name]} \\\\ \\vspace{1pt}
    \\small [Phone] $|$ \\href{mailto:[email]}{\\underline{[email]}} $|$ 
    \\href{[linkedin]}{\\underline{linkedin.com/in/[username]}} $|$
    \\href{[github]}{\\underline{github.com/[username]}}
\\end{center}

%% EDUCATION SECTION
\\section{Education}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {[University Name]}{[Start Date] -- [End Date]}
      {[Degree] in [Major]}{[Location]}
  \\resumeSubHeadingListEnd

%% TECHNICAL SKILLS SECTION
\\section{Technical Skills}
 \\begin{itemize}[leftmargin=0.15in, label={}]
    \\small{\\item{
     \\textbf{Languages}{: [Programming Languages]} \\\\
     \\textbf{Frameworks}{: [Frameworks and Libraries]} \\\\
     \\textbf{Developer Tools}{: [Tools and Software]} \\\\
     \\textbf{Libraries}{: [Additional Libraries]}
    }}
 \\end{itemize}

%% EXPERIENCE SECTION
\\section{Experience}
  \\resumeSubHeadingListStart
    \\resumeSubheading
      {[Job Title]}{[Start Date] -- [End Date]}
      {[Company Name]}{[Location]}
      \\resumeItemListStart
        \\resumeItem{[Achievement or responsibility with quantified results]}
        \\resumeItem{[Achievement or responsibility with quantified results]}
      \\resumeItemListEnd
  \\resumeSubHeadingListEnd

%% PROJECTS SECTION
\\section{Projects}
    \\resumeSubHeadingListStart
      \\resumeProjectHeading
          {\\textbf{[Project Name]} \\;|\\; \\emph{[Technologies Used]}}{[Date]}
          \\resumeItemListStart
            \\resumeItem{[Project description or achievement with specific metrics]}
          \\resumeItemListEnd
    \\resumeSubHeadingListEnd

\\end{document}
"""
    
    return jsonify({
        "template": jakes_template.strip(),
        "name": "Jake's Resume Template",
        "description": "Clean, ATS-friendly resume template"
    })

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True)
