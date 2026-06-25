from __future__ import annotations

import pytest
from pydantic import BaseModel, ValidationError
from core.schema_validator import SchemaValidator, SchemaValidationError


class SimpleModel(BaseModel):
    name: str
    value: int


class NestedModel(BaseModel):
    id: int
    label: str


@pytest.fixture
def validator():
    return SchemaValidator()


def test_register_and_validate_success(validator):
    validator.register("simple", SimpleModel)
    result = validator.validate("simple", {"name": "test", "value": 42})
    assert result["status"] == "ok"
    assert result["schema"] == "simple"
    assert result["data"] == {"name": "test", "value": 42}


def test_validate_unknown_schema(validator):
    with pytest.raises(KeyError, match="Schema 'unknown' is not registered"):
        validator.validate("unknown", {"foo": "bar"})


def test_validate_validation_error(validator):
    validator.register("simple", SimpleModel)
    with pytest.raises(SchemaValidationError) as exc_info:
        validator.validate("simple", {"name": "test"})
    assert exc_info.value.model_name == "simple"
    assert len(exc_info.value.errors) > 0


def test_schema_validation_error_attributes():
    errors = [{"loc": "value", "msg": "Input should be a valid integer", "type": "int_parsing"}]
    err = SchemaValidationError("simple", errors)
    assert err.model_name == "simple"
    assert err.errors == errors
    assert "Validation failed for simple" in str(err)


def test_try_parse_success(validator):
    validator.register("nested", NestedModel)
    result = validator.try_parse("nested", {"id": 1, "label": "x"})
    assert result["status"] == "ok"


def test_try_parse_key_error(validator):
    result = validator.try_parse("missing", {})
    assert result["status"] == "error"
    assert "Schema 'missing' is not registered" in result["error"]


def test_try_parse_validation_error(validator):
    validator.register("simple", SimpleModel)
    result = validator.try_parse("simple", {"name": "test"})
    assert result["status"] == "error"
    assert "simple" in result["error"]


def test_validate_with_retry_success_first_attempt(validator):
    validator.register("simple", SimpleModel)
    result = validator.validate_with_retry("simple", {"name": "ok", "value": 1})
    assert result["status"] == "ok"


def test_validate_with_retry_fails_after_attempts(validator):
    validator.register("simple", SimpleModel)
    result = validator.validate_with_retry("simple", {"name": "bad"}, max_attempts=2)
    assert result["status"] == "error"
    assert result["schema"] == "simple"
    assert result["attempts"] == 2


def test_prepare_for_retry_success(validator):
    validator.register("simple", SimpleModel)
    result = validator._prepare_for_retry("simple", {"name": "ok", "value": 1}, 1)
    assert result["status"] == "ok"


def test_prepare_for_retry_failure(validator):
    validator.register("simple", SimpleModel)
    result = validator._prepare_for_retry("simple", {"name": "bad"}, 1)
    assert result["status"] == "retry"
    assert result["attempt"] == 1
    assert "last_error" in result


def test_validate_nested_model(validator):
    validator.register("nested", NestedModel)
    result = validator.validate("nested", {"id": 99, "label": "item"})
    assert result["status"] == "ok"
    assert result["data"]["id"] == 99
