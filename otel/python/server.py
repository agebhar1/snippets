import time

import uvicorn
from fastapi import FastAPI
from opentelemetry import trace

app = FastAPI()
tracer = trace.get_tracer(__name__)


@app.get("/")
async def hello() -> dict:
    current_span = trace.get_current_span()
    current_span.set_attribute("attribute", "attribute-value")
    current_span.add_event("event", {"a": "value a", "b": "value b"})

    with tracer.start_as_current_span("parent"):
        time.sleep(0.1)
        with tracer.start_as_current_span("child"):
            time.sleep(0.2)
        time.sleep(0.1)

    return {"message": "Hello World"}


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)  # , log_config=None)
