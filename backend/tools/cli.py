# backend/tools/cli.py
# Production Headless Zero-Cost Terminal AI Agent for SupremeAI 2.0
# বাংলা মন্তব্য: ইন্টারঅ্যাক্টিভ হেডলেস টার্মিনাল মোড ও ফ্রি-টিয়ার মডেল ফলব্যাক কমান্ড হ্যান্ডলার।

import os
import sys

import typer
from rich.console import Console
from rich.prompt import Prompt
from rich.table import Table

# Add project root to sys path
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from brain.langgraph_agent import SupremeOrchestrator
from core.admin_god import AdminGodLayer
from core.universal_rules import UniversalRulesEngine

cli_app = typer.Typer(help="SupremeAI 2.0 Command Line Interface & Headless Agent")
console = Console()


@cli_app.command()
def ask(
    task: str = typer.Option(..., "--task", "-t", help="Task prompt for the agent"),
    task_type: str = typer.Option(
        "general", "--type", "-y", help="Task type (coding, image_generation, etc.)"
    ),
):
    """Asks SupremeAI 2.0 to solve a task in single execution mode."""
    console.print(
        f"[bold blue]Submitting task to SupremeAI Master Orchestrator:[/bold blue] {task}"
    )

    rules = UniversalRulesEngine()
    admin = AdminGodLayer(rules)
    orchestrator = SupremeOrchestrator(admin)

    response = orchestrator.execute_task(task, task_type)

    if "Blocked" in response.get("result", ""):
        console.print(
            f"[bold red]EXECUTION BLOCKED:[/bold red] {response.get('result')}"
        )
    else:
        console.print("[bold green]Response Result:[/bold green]")
        console.print(response.get("result", "No response output."))
        console.print(
            f"[yellow]Cost accumulated: ${response.get('cost', 0.0)}[/yellow]"
        )


@cli_app.command()
def repl():
    """Starts interactive Headless Zero-Cost Terminal Agent REPL session.

    বাংলা মন্তব্য: যেকোনো GUI ছাড়াই সোজা টার্মিনাল থেকে ইন্টারঅ্যাক্টিভ AI এজেন্ট সেশন চালু করে।
    """
    console.print(
        "[bold cyan]════════════════════════════════════════════════════════════════[/bold cyan]"
    )
    console.print(
        "[bold green]🤖 SupremeAI 2.0 Headless Terminal Agent Mode (Zero-Cost Active)[/bold green]"
    )
    console.print("[dim]Type 'exit' or 'quit' to terminate session.[/dim]")
    console.print(
        "[bold cyan]════════════════════════════════════════════════════════════════[/bold cyan]\n"
    )

    rules = UniversalRulesEngine()
    admin = AdminGodLayer(rules)
    orchestrator = SupremeOrchestrator(admin)

    while True:
        try:
            user_input = Prompt.ask("[bold yellow]supremeai>[/bold yellow]").strip()
            if not user_input:
                continue
            if user_input.lower() in ("exit", "quit", "q"):
                console.print("[bold red]Session ended. Goodbye![/bold red]")
                break

            with console.status(
                "[bold cyan]Thinking & Executing (Zero-Cost Routing)...[/bold cyan]"
            ):
                response = orchestrator.execute_task(user_input, "general")

            console.print("[bold green]Agent Response:[/bold green]")
            console.print(response.get("result", "No output generated."))
            console.print()
        except KeyboardInterrupt:
            console.print("\n[bold red]Interrupted by user. Quitting REPL.[/bold red]")
            break
        except Exception as e:
            console.print(f"[bold red]Error:[/bold red] {e}")


@cli_app.command()
def rules():
    """Lists all Constitutional Rules currently active."""
    rules_engine = UniversalRulesEngine()
    current_rules = rules_engine.rules

    table = Table(title="SupremeAI 2.0 Constitutional Rules")
    table.add_column("Rule Area", style="cyan")
    table.add_column("Configuration", style="magenta")

    for area, config in current_rules.items():
        table.add_row(area, str(config))

    console.print(table)


def parse_args():
    import argparse

    parser = argparse.ArgumentParser(description="SupremeAI CLI Parser")
    parser.add_argument("--task", "-t", default="", help="Task prompt")
    parser.add_argument("--type", "-y", default="general", help="Task type")
    return parser.parse_args()


if __name__ == "__main__":
    cli_app()
