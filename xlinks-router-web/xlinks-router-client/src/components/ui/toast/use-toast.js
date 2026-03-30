import { ref } from "vue";

const TOAST_LIMIT = 1;
const TOAST_REMOVE_DELAY = 1000000;

const count = ref(0);

function genId() {
  count.value = (count.value + 1) % Number.MAX_VALUE;
  return count.value.toString();
}

const toasts = ref([]);

const addToasts = (props) => {
  const id = genId();

  const update = (props) => {
    const index = toasts.value.findIndex((t) => t.id === id);
    if (index !== -1) {
      toasts.value[index] = { ...toasts.value[index], ...props };
    }
  };

  const dismiss = () => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  };

  toasts.value = [
    {
      id,
      open: true,
      onOpenChange: (open) => {
        if (!open) dismiss();
      },
      ...props,
    },
    ...toasts.value,
  ].slice(0, TOAST_LIMIT);

  return {
    id,
    dismiss,
    update,
  };
};

function toast({ ...props }) {
  const { id, dismiss, update } = addToasts(props);

  return {
    id,
    dismiss,
    update,
  };
}

export { useToast, toast };

function useToast() {
  return {
    toasts,
    toast,
    dismiss: (toastId) => {
      toasts.value = toasts.value.filter((t) => t.id !== (toastId || t.id));
    },
  };
}
