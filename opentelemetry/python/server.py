import logging
from functools import wraps
from typing import Callable, Dict, Any

import aiohttp
import anyio
import uvicorn
from fastapi import FastAPI
from opentelemetry import trace

app = FastAPI()
logger = logging.getLogger("uvicorn")


# https://medium.com/better-programming/using-decorators-to-instrument-python-code-with-opentelemetry-traces-d7f1c7d6f632
class TracingDecoratorOptions:
    class NamingSchemes:
        @staticmethod
        def function_qualified_name(func: Callable):
            return func.__qualname__

        default_scheme = function_qualified_name

    naming_scheme: Callable[[Callable], str] = NamingSchemes.default_scheme
    default_attributes: Dict[str, str] = {}

    @staticmethod
    def set_naming_scheme(naming_scheme: Callable[[Callable], str]) -> None:
        TracingDecoratorOptions.naming_scheme = naming_scheme


# https://medium.com/better-programming/using-decorators-to-instrument-python-code-with-opentelemetry-traces-d7f1c7d6f632
def instrument(_func=None, *, span_name: str = "", record_exception: bool = True,
               attributes: Dict[str, str] | None = None, existing_tracer: trace.Tracer | None = None):
    def span_decorator(func):
        tracer = existing_tracer or trace.get_tracer(func.__module__)

        def _set_attributes(span, attributes_dict):
            if attributes_dict:
                for att in attributes_dict:
                    span.set_attribute(att, attributes_dict[att])

        @wraps(func)
        async def wrap_with_span(*args, **kwargs):
            name = span_name or TracingDecoratorOptions.naming_scheme(func)
            with tracer.start_as_current_span(name, record_exception=record_exception) as span:
                _set_attributes(span, attributes)
                return await func(*args, **kwargs)

        return wrap_with_span

    if _func is None:
        return span_decorator
    else:
        return span_decorator(_func)


@instrument
async def fetch(url: str) -> dict[str, Any]:
    async with aiohttp.ClientSession() as session, session.get(url) as response:
        return await response.json()


@instrument
async def h() -> None:
    await anyio.sleep(0.5)


@instrument
async def g() -> str:
    await h()
    response = await fetch("https://hackmyip.com/api/ip")
    await h()
    return response["data"]["location"]["city"]


@instrument
async def f() -> str:
    await anyio.sleep(0.25)
    response = await g()
    await anyio.sleep(0.25)
    return response


@app.get("/")
async def hello() -> dict:
    current_span = trace.get_current_span()
    current_span.set_attribute("attribute", "attribute-value")
    current_span.add_event("event", {"a": "value a", "b": "value b"})

    value = await f()

    return {"message": f"Hello {value}"}


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)  # , log_config=None)
