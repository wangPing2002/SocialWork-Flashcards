#!/usr/bin/env python3
"""Compatibility alias after V32 active-answer migration. Run the current V32 gate."""
from pathlib import Path
import runpy
runpy.run_path(str(Path(__file__).with_name('verify_v32_integrated.py')),run_name='__main__')
