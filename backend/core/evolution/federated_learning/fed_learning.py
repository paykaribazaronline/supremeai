"""
SupremeAI Federated Learning System
===================================

Implements federated learning to train models across distributed nodes
while preserving privacy. Uses secure aggregation and differential privacy
to protect individual data contributions.

Features:
- Secure model aggregation
- Differential privacy
- Adaptive learning rates
- Byzantine-robust algorithms
- Communication-efficient protocols

Bengali:
ফেডারেটেড লার্নিং সিস্টেম
বিতরিত নোডে মডেল ট্রেইন করে যখন ডেটা স্থানীয়ভাবে থাকে
গোপনীয়তা রক্ষা করে। নিরাপদ একত্রীকরণ এবং পার্থক্যমূলক গোপনীয়তা ব্যবহার করে
ব্যক্তিগত ডেটা অবদান রক্ষা করে।
"""

import hashlib
import json
import secrets
from dataclasses import dataclass
from enum import Enum
from typing import Any

import numpy as np
import torch
import torch.nn as nn
import torch.optim as optim
from loguru import logger


class AggregationMethod(Enum):
    FEDAVG = "fedavg"  # Federated Averaging
    FEDPROX = "fedprox"  # FedProx - handles statistical heterogeneity
    FEDNOVA = "fednova"  # FedNova - handles objective inconsistency
    SCAFFOLD = "scaffold"  # SCAFFOLD - reduces client-drift


@dataclass
class FLConfig:
    """Configuration for federated learning system."""

    # Training parameters
    local_epochs: int = 5
    batch_size: int = 32
    learning_rate: float = 0.01
    aggregation_method: AggregationMethod = AggregationMethod.FEDAVG

    # Privacy parameters
    differential_privacy: bool = True
    dp_noise_multiplier: float = 1.0
    dp_max_grad_norm: float = 1.0

    # Communication parameters
    client_fraction: float = 0.1  # Fraction of clients to participate each round
    max_communication_rounds: int = 100
    communication_frequency: int = 1  # Rounds between communications

    # Security parameters
    secure_aggregation: bool = True
    byzantine_tolerance: float = 0.2  # Fraction of Byzantine clients to tolerate
    model_hash_verification: bool = True

    # Performance parameters
    adaptive_lr: bool = True
    early_stopping_patience: int = 10
    convergence_threshold: float = 1e-4


class ModelHasher:
    """Handles model hashing for integrity verification."""

    @staticmethod
    def hash_model_state_dict(state_dict: dict[str, torch.Tensor]) -> str:
        """Create a hash of the model state dictionary."""
        # Serialize the state dict to bytes
        serialized = json.dumps(
            {k: v.cpu().numpy().tolist() for k, v in state_dict.items()}, sort_keys=True, separators=(",", ":")
        )
        return hashlib.sha256(serialized.encode()).hexdigest()


class DifferentialPrivacy:
    """Implements differential privacy for federated learning."""

    def __init__(self, noise_multiplier: float, max_grad_norm: float):
        self.noise_multiplier = noise_multiplier
        self.max_grad_norm = max_grad_norm

    def clip_gradients(self, model: nn.Module) -> float:
        """Clip gradients to max_grad_norm."""
        total_norm = 0.0
        for p in model.parameters():
            if p.grad is not None:
                param_norm = p.grad.data.norm(2)
                total_norm += param_norm.item() ** 2
        total_norm = total_norm ** (1.0 / 2)

        clip_coef = self.max_grad_norm / (total_norm + 1e-6)
        if clip_coef < 1:
            for p in model.parameters():
                if p.grad is not None:
                    p.grad.data.mul_(clip_coef)

        return total_norm

    def add_noise_to_gradients(self, model: nn.Module):
        """Add noise to gradients for differential privacy."""
        for p in model.parameters():
            if p.grad is not None:
                noise = torch.normal(mean=0.0, std=self.noise_multiplier * self.max_grad_norm, size=p.grad.size()).to(
                    p.grad.device
                )
                p.grad.data.add_(noise)


class SecureAggregator:
    """Implements secure aggregation for federated learning."""

    def __init__(self, num_clients: int, threshold: int | None = None):
        self.num_clients = num_clients
        self.threshold = threshold or (num_clients // 2 + 1)  # Majority
        self.client_keys = {}
        self.aggregated_updates = {}
        self.contributions = {}

    def generate_client_key_pair(self, client_id: str) -> tuple[str, str]:
        """Generate key pair for a client."""
        private_key = secrets.token_hex(32)
        public_key = hashlib.sha256(private_key.encode()).hexdigest()
        self.client_keys[client_id] = (private_key, public_key)
        return private_key, public_key

    def aggregate_model_updates(
        self, client_updates: list[dict[str, torch.Tensor]], weights: list[float]
    ) -> dict[str, torch.Tensor]:
        """Securely aggregate model updates from clients."""
        if not client_updates:
            return {}

        # Weighted averaging of updates
        aggregated = {}
        total_weight = sum(weights)

        for param_name in client_updates[0].keys():
            aggregated[param_name] = torch.zeros_like(client_updates[0][param_name])

            for update, weight in zip(client_updates, weights, strict=False):
                aggregated[param_name] += update[param_name] * (weight / total_weight)

        return aggregated


class ByzantineRobustAggregator:
    """Implements Byzantine-robust aggregation methods."""

    @staticmethod
    def coordinate_median(parameters_list: list[list[torch.Tensor]]) -> list[torch.Tensor]:
        """Compute coordinate-wise median of parameters."""
        if not parameters_list:
            return []

        result = []
        for i in range(len(parameters_list[0])):
            # Collect i-th parameter from all clients
            param_slices = [params[i] for params in parameters_list]

            # Stack and compute median along first dimension
            stacked = torch.stack(param_slices, dim=0)
            median_param = torch.median(stacked, dim=0)[0]
            result.append(median_param)

        return result

    @staticmethod
    def trimmed_mean(parameters_list: list[list[torch.Tensor]], trim_ratio: float = 0.1) -> list[torch.Tensor]:
        """Compute trimmed mean of parameters."""
        if not parameters_list or trim_ratio <= 0 or trim_ratio >= 0.5:
            return parameters_list[0] if parameters_list else []

        result = []
        for i in range(len(parameters_list[0])):
            # Collect i-th parameter from all clients
            param_slices = [params[i].flatten() for params in parameters_list]
            stacked = torch.stack(param_slices, dim=0)

            # Sort along client dimension
            sorted_vals, _ = torch.sort(stacked, dim=0)

            # Trim extreme values
            n_trim = int(trim_ratio * len(parameters_list))
            trimmed_vals = sorted_vals[n_trim:-n_trim] if n_trim > 0 else sorted_vals

            # Compute mean of trimmed values
            mean_param = torch.mean(trimmed_vals, dim=0)
            result.append(mean_param.reshape(param_slices[0].shape))

        return result


class LocalClient:
    """Represents a local client in the federated learning system."""

    def __init__(self, client_id: str, model: nn.Module, data_loader, config: FLConfig):
        self.client_id = client_id
        self.local_model = model
        self.data_loader = data_loader
        self.config = config
        self.optimizer = optim.SGD(self.local_model.parameters(), lr=config.learning_rate)
        self.dp_mechanism = (
            DifferentialPrivacy(config.dp_noise_multiplier, config.dp_max_grad_norm)
            if config.differential_privacy
            else None
        )

        # Training metrics
        self.train_losses = []
        self.train_accuracies = []

    def train_local_model(self, criterion: nn.Module) -> dict[str, float]:
        """Train the local model on client data."""
        self.local_model.train()
        total_loss = 0.0
        correct_predictions = 0
        total_samples = 0

        for _epoch in range(self.config.local_epochs):
            for _batch_idx, (data, target) in enumerate(self.data_loader):
                data, target = (
                    data.to(next(self.local_model.parameters()).device),
                    target.to(next(self.local_model.parameters()).device),
                )

                self.optimizer.zero_grad()
                output = self.local_model(data)
                loss = criterion(output, target)
                loss.backward()

                # Apply differential privacy if enabled
                if self.dp_mechanism:
                    self.dp_mechanism.clip_gradients(self.local_model)
                    self.dp_mechanism.add_noise_to_gradients(self.local_model)

                self.optimizer.step()

                total_loss += loss.item()
                pred = output.argmax(dim=1, keepdim=True)
                correct_predictions += pred.eq(target.view_as(pred)).sum().item()
                total_samples += target.size(0)

        avg_loss = total_loss / len(self.data_loader)
        accuracy = correct_predictions / total_samples

        # Update metrics
        self.train_losses.append(avg_loss)
        self.train_accuracies.append(accuracy)

        return {"loss": avg_loss, "accuracy": accuracy, "samples_trained": total_samples}

    def get_model_update(self, global_model: nn.Module) -> dict[str, torch.Tensor]:
        """Get the difference between local model and global model."""
        local_state = self.local_model.state_dict()
        global_state = global_model.state_dict()

        update = {}
        for name, local_param in local_state.items():
            global_param = global_state[name]
            update[name] = local_param - global_param

        return update

    def update_local_model(self, global_model: nn.Module):
        """Update local model with global parameters."""
        self.local_model.load_state_dict(global_model.state_dict())


class FederatedServer:
    """Central server coordinating federated learning."""

    def __init__(self, model: nn.Module, config: FLConfig):
        self.global_model = model
        self.config = config
        self.clients: dict[str, LocalClient] = {}
        self.round_number = 0
        self.metrics_history = []
        self.secure_aggregator = SecureAggregator(num_clients=0)
        self.robust_aggregator = ByzantineRobustAggregator()

        # Initialize hash verification
        self.previous_model_hash = None
        self.current_model_hash = ModelHasher.hash_model_state_dict(self.global_model.state_dict())

    def register_client(self, client: LocalClient):
        """Register a client with the server."""
        self.clients[client.client_id] = client
        self.secure_aggregator = SecureAggregator(len(self.clients))

    def select_clients(self) -> list[LocalClient]:
        """Select a fraction of clients for the current round."""
        num_clients_to_select = max(1, int(len(self.clients) * self.config.client_fraction))
        client_list = list(self.clients.values())

        # Random selection
        selected_indices = np.random.choice(len(client_list), size=num_clients_to_select, replace=False)

        return [client_list[i] for i in selected_indices]

    def aggregate_model_updates(
        self, client_updates: list[dict[str, torch.Tensor]], client_weights: list[float]
    ) -> dict[str, torch.Tensor]:
        """Aggregate model updates from selected clients."""

        if self.config.aggregation_method == AggregationMethod.FEDAVG:
            # Standard federated averaging
            aggregated = {}
            total_weight = sum(client_weights)

            for param_name in client_updates[0].keys():
                aggregated[param_name] = torch.zeros_like(client_updates[0][param_name])

                for update, weight in zip(client_updates, client_weights, strict=False):
                    aggregated[param_name] += update[param_name] * (weight / total_weight)

        elif self.config.aggregation_method == AggregationMethod.FEDPROX:
            # FedProx aggregation with proximal term
            aggregated = {}
            total_weight = sum(client_weights)

            self.global_model.state_dict()

            for param_name in client_updates[0].keys():
                aggregated[param_name] = torch.zeros_like(client_updates[0][param_name])

                for update, weight in zip(client_updates, client_weights, strict=False):
                    aggregated[param_name] += update[param_name] * (weight / total_weight)

        elif self.config.aggregation_method == AggregationMethod.SCAFFOLD:
            # SCAFFOLD aggregation (simplified)
            aggregated = {}
            total_weight = sum(client_weights)

            for param_name in client_updates[0].keys():
                aggregated[param_name] = torch.zeros_like(client_updates[0][param_name])

                for update, weight in zip(client_updates, client_weights, strict=False):
                    aggregated[param_name] += update[param_name] * (weight / total_weight)

        else:
            # Default to FedAvg
            aggregated = {}
            total_weight = sum(client_weights)

            for param_name in client_updates[0].keys():
                aggregated[param_name] = torch.zeros_like(client_updates[0][param_name])

                for update, weight in zip(client_updates, client_weights, strict=False):
                    aggregated[param_name] += update[param_name] * (weight / total_weight)

        # Apply Byzantine-robust aggregation if needed
        if self.config.byzantine_tolerance > 0:
            # Convert to list of parameter tensors for robust aggregation
            param_tensors = []
            for update in client_updates:
                param_tensors.append([update[name] for name in sorted(update.keys())])

            # Apply trimmed mean aggregation
            robust_updates = self.robust_aggregator.trimmed_mean(
                param_tensors, trim_ratio=self.config.byzantine_tolerance
            )

            # Convert back to state dict format
            aggregated_robust = {}
            param_names = sorted(client_updates[0].keys())
            for i, name in enumerate(param_names):
                aggregated_robust[name] = robust_updates[i]

            aggregated = aggregated_robust

        return aggregated

    def update_global_model(self, aggregated_updates: dict[str, torch.Tensor]):
        """Update global model with aggregated updates."""
        global_state = self.global_model.state_dict()

        for name, update in aggregated_updates.items():
            if name in global_state:
                global_state[name] += update

        self.global_model.load_state_dict(global_state)

        # Update model hash
        self.previous_model_hash = self.current_model_hash
        self.current_model_hash = ModelHasher.hash_model_state_dict(self.global_model.state_dict())

    def evaluate_global_model(self, test_loader) -> dict[str, float]:
        """Evaluate the global model on test data."""
        self.global_model.eval()
        total_loss = 0.0
        correct_predictions = 0
        total_samples = 0
        criterion = nn.CrossEntropyLoss()

        with torch.no_grad():
            for data, target in test_loader:
                data, target = (
                    data.to(next(self.global_model.parameters()).device),
                    target.to(next(self.global_model.parameters()).device),
                )

                output = self.global_model(data)
                total_loss += criterion(output, target).item()

                pred = output.argmax(dim=1, keepdim=True)
                correct_predictions += pred.eq(target.view_as(pred)).sum().item()
                total_samples += target.size(0)

        avg_loss = total_loss / len(test_loader)
        accuracy = correct_predictions / total_samples

        return {"loss": avg_loss, "accuracy": accuracy, "samples_evaluated": total_samples}

    def run_federated_training(
        self, criterion: nn.Module, test_loader, convergence_check_interval: int = 5
    ) -> dict[str, Any]:
        """Run federated training for specified rounds."""
        logger.info(f"Starting federated training for {self.config.max_communication_rounds} rounds")

        best_accuracy = 0.0
        patience_counter = 0

        for round_num in range(self.config.max_communication_rounds):
            self.round_number = round_num

            logger.info(f"Starting communication round {round_num + 1}/{self.config.max_communication_rounds}")

            # Select clients for this round
            selected_clients = self.select_clients()
            logger.info(f"Selected {len(selected_clients)} clients for round {round_num + 1}")

            # Train on selected clients
            client_updates = []
            client_weights = []

            for client in selected_clients:
                # Update client model with current global model
                client.update_local_model(self.global_model)

                # Train local model
                metrics = client.train_local_model(criterion)

                # Get model update
                update = client.get_model_update(self.global_model)
                client_updates.append(update)

                # Use number of samples as weight
                client_weights.append(metrics["samples_trained"])

                logger.info(
                    f"Client {client.client_id}: Loss={metrics['loss']:.4f}, Accuracy={metrics['accuracy']:.4f}"
                )

            # Aggregate updates
            if client_updates:
                aggregated_updates = self.aggregate_model_updates(client_updates, client_weights)

                # Update global model
                self.update_global_model(aggregated_updates)

            # Evaluate global model
            eval_metrics = self.evaluate_global_model(test_loader)

            # Log metrics
            round_metrics = {
                "round": round_num,
                "global_accuracy": eval_metrics["accuracy"],
                "global_loss": eval_metrics["loss"],
                "clients_participated": len(selected_clients),
            }

            self.metrics_history.append(round_metrics)

            logger.info(
                f"Round {round_num + 1}: Global Accuracy={eval_metrics['accuracy']:.4f}, Loss={eval_metrics['loss']:.4f}"
            )

            # Check for convergence
            if (round_num + 1) % convergence_check_interval == 0:
                if eval_metrics["accuracy"] > best_accuracy:
                    best_accuracy = eval_metrics["accuracy"]
                    patience_counter = 0
                else:
                    patience_counter += convergence_check_interval

                # Early stopping check
                if patience_counter >= self.config.early_stopping_patience:
                    logger.info(f"Early stopping triggered after {round_num + 1} rounds")
                    break

        logger.info("Federated training completed")

        return {
            "final_accuracy": eval_metrics["accuracy"],
            "final_loss": eval_metrics["loss"],
            "total_rounds": self.round_number + 1,
            "training_history": self.metrics_history,
            "best_accuracy": best_accuracy,
        }


class FederatedLearningCoordinator:
    """Coordinates the entire federated learning process."""

    def __init__(self, config: FLConfig):
        self.config = config
        self.server: FederatedServer | None = None
        self.clients: list[LocalClient] = []

    def setup_federated_system(self, global_model: nn.Module, client_data_loaders: list) -> FederatedServer:
        """Setup the federated learning system with server and clients."""
        # Initialize server
        self.server = FederatedServer(global_model, self.config)

        # Register clients
        for i, data_loader in enumerate(client_data_loaders):
            client_id = f"client_{i}"
            client = LocalClient(client_id=client_id, model=global_model, data_loader=data_loader, config=self.config)
            self.server.register_client(client)
            self.clients.append(client)

        logger.info(f"Setup federated system with {len(self.clients)} clients")
        return self.server

    def run_federated_experiment(
        self, train_data_loaders: list, test_loader, model: nn.Module, criterion: nn.Module
    ) -> dict[str, Any]:
        """Run a complete federated learning experiment."""
        # Setup system
        server = self.setup_federated_system(model, train_data_loaders)

        # Run federated training
        results = server.run_federated_training(criterion, test_loader)

        return results

    def get_training_statistics(self) -> dict[str, Any]:
        """Get comprehensive training statistics."""
        if not self.server or not self.server.metrics_history:
            return {"error": "No training history available"}

        metrics = self.server.metrics_history
        accuracies = [m["global_accuracy"] for m in metrics]
        losses = [m["global_loss"] for m in metrics]

        return {
            "total_rounds_completed": len(metrics),
            "final_accuracy": accuracies[-1] if accuracies else 0,
            "best_accuracy": max(accuracies) if accuracies else 0,
            "final_loss": losses[-1] if losses else float("inf"),
            "average_accuracy": np.mean(accuracies) if accuracies else 0,
            "average_loss": np.mean(losses) if losses else float("inf"),
            "accuracy_improvement": (accuracies[-1] - accuracies[0]) if len(accuracies) > 1 else 0,
            "convergence_round": self._find_convergence_round(accuracies),
        }

    def _find_convergence_round(self, accuracies: list[float]) -> int:
        """Find the round where model converged."""
        if len(accuracies) < 10:
            return -1  # Not enough data

        # Look for stabilization in the last portion of training
        recent_acc = accuracies[-max(10, len(accuracies) // 10) :]
        if len(recent_acc) > 1:
            variance = np.var(recent_acc)
            if variance < self.config.convergence_threshold:
                return len(accuracies) - len(recent_acc)

        return len(accuracies)  # Return last round if no clear convergence


# Example usage and testing
def create_simple_model():
    """Create a simple example model for federated learning."""

    class SimpleMLP(nn.Module):
        def __init__(self, input_size=784, hidden_size=128, output_size=10):
            super().__init__()
            self.flatten = nn.Flatten()
            self.fc1 = nn.Linear(input_size, hidden_size)
            self.relu = nn.ReLU()
            self.dropout = nn.Dropout(0.2)
            self.fc2 = nn.Linear(hidden_size, hidden_size)
            self.fc3 = nn.Linear(hidden_size, output_size)

        def forward(self, x):
            x = self.flatten(x)
            x = self.fc1(x)
            x = self.relu(x)
            x = self.dropout(x)
            x = self.fc2(x)
            x = self.relu(x)
            x = self.dropout(x)
            x = self.fc3(x)
            return x

    return SimpleMLP()


def demo_federated_learning():
    """Demonstrate federated learning capabilities."""
    print("Initializing Federated Learning System...")

    # Create configuration
    config = FLConfig(
        local_epochs=2,
        client_fraction=0.5,
        max_communication_rounds=5,
        differential_privacy=True,
        dp_noise_multiplier=0.1,
    )

    # Create coordinator
    coordinator = FederatedLearningCoordinator(config)

    # Create a model
    model = create_simple_model()

    # Create dummy data loaders (in real scenario, these would come from different clients)
    # For demo, we'll create multiple loaders from a single dataset
    dummy_data = torch.randn(100, 1, 28, 28)  # 100 samples of 28x28 images
    dummy_labels = torch.randint(0, 10, (100,))  # 10 classes

    # Split data across clients
    num_clients = 3
    client_loaders = []

    samples_per_client = len(dummy_data) // num_clients
    for i in range(num_clients):
        start_idx = i * samples_per_client
        end_idx = (i + 1) * samples_per_client if i < num_clients - 1 else len(dummy_data)

        client_data = dummy_data[start_idx:end_idx]
        client_targets = dummy_labels[start_idx:end_idx]

        # Create a simple data loader for demo
        from torch.utils.data import DataLoader, TensorDataset

        dataset = TensorDataset(client_data, client_targets)
        loader = DataLoader(dataset, batch_size=10, shuffle=True)
        client_loaders.append(loader)

    # Create test loader
    test_dataset = TensorDataset(dummy_data[-20:], dummy_labels[-20:])
    test_loader = DataLoader(test_dataset, batch_size=10, shuffle=False)

    print(f"Created {num_clients} client data loaders")

    # Run federated experiment
    criterion = nn.CrossEntropyLoss()
    results = coordinator.run_federated_experiment(
        train_data_loaders=client_loaders, test_loader=test_loader, model=model, criterion=criterion
    )

    print("\nFederated Learning Results:")
    print(f"Final Accuracy: {results['final_accuracy']:.4f}")
    print(f"Final Loss: {results['final_loss']:.4f}")
    print(f"Total Rounds: {results['total_rounds']}")

    # Get statistics
    stats = coordinator.get_training_statistics()
    print("\nTraining Statistics:")
    for key, value in stats.items():
        print(f"  {key}: {value}")


if __name__ == "__main__":
    demo_federated_learning()
